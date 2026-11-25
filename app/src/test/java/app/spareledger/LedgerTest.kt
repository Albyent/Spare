package app.spareledger

import app.spareledger.data.Account
import app.spareledger.data.AccountKind
import app.spareledger.data.Category
import app.spareledger.data.CategoryKind
import app.spareledger.data.EntryKind
import app.spareledger.data.Glyph
import app.spareledger.data.LedgerConfig
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.Money
import app.spareledger.data.MonthlyBudget
import app.spareledger.data.MonthlyLimit
import app.spareledger.data.balances
import app.spareledger.data.limitsFor
import app.spareledger.data.spending
import app.spareledger.data.validateEntry
import app.spareledger.data.writeLedgerCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class LedgerTest {
    private val today = LocalDate.of(2026, 11, 25)
    private val month = YearMonth.from(today)
    private val bank = Account("Bank", AccountKind.BANK, 50_000, id = 1)
    private val card = Account("Card", AccountKind.CARD, 0, id = 2)
    private val groceries = Category("Groceries", CategoryKind.EXPENSE, Glyph.GROCERIES, id = 1)
    private val salary = Category("Salary", CategoryKind.INCOME, Glyph.INCOME, id = 2)
    private val purchase = LedgerEntry(
        EntryKind.EXPENSE, 12_500, today.minusDays(2).toEpochDay(), card.id, categoryId = groceries.id, id = 1,
    )
    private val payment = LedgerEntry(
        EntryKind.TRANSFER, 12_500, today.toEpochDay(), bank.id, destinationId = card.id, id = 2,
    )

    @Test
    fun cardPaymentUpdatesBothBalancesAndCountsPurchaseOnce() {
        val ledger = ledger(purchase, payment)
        assertEquals(12_500L, ledger.spending(month, today).spent)
        assertEquals(mapOf(bank.id to 37_500L, card.id to 0L), ledger.balances(today))
    }

    @Test
    fun editingAndRemovingATransferChangesBothSidesTogether() {
        val edited = ledger(purchase, payment.copy(minor = 7_500))
        assertEquals(mapOf(bank.id to 42_500L, card.id to -5_000L), edited.balances(today))
        assertEquals(12_500L, edited.spending(month, today).spent)
        assertEquals(mapOf(bank.id to 50_000L, card.id to -12_500L), ledger(purchase).balances(today))
    }

    @Test
    fun refundsReduceSpendingOnTheDateTheRefundWasReceived() {
        val refund = purchase.copy(kind = EntryKind.REFUND, minor = 2_000, day = today.toEpochDay(), id = 3)
        val oldPurchase = purchase.copy(day = month.minusMonths(1).atEndOfMonth().toEpochDay())
        val income = LedgerEntry(
            EntryKind.INCOME, 30_000, today.toEpochDay(), bank.id, categoryId = salary.id, id = 4,
        )
        val future = purchase.copy(id = 5, day = today.plusDays(1).toEpochDay())
        val data = ledger(oldPurchase, refund, income, future).copy(
            budgets = listOf(MonthlyBudget(month.toString(), 20_000)),
        )
        assertEquals(-2_000L, data.spending(month, today).spent)
        assertEquals(22_000L, data.spending(month, today).remaining)
        assertEquals(12_500L, data.spending(month.minusMonths(1), today).spent)
        assertEquals(mapOf(bank.id to 80_000L, card.id to -10_500L), data.balances(today))
    }

    @Test
    fun monthlyBudgetUsesSpendingOnlyAndBelongsToTheSelectedMonth() {
        val income = LedgerEntry(
            EntryKind.INCOME, 30_000, today.toEpochDay(), bank.id, categoryId = salary.id, id = 3,
        )
        val previous = month.minusMonths(1)
        val data = ledger(purchase, payment, income).copy(
            budgets = listOf(MonthlyBudget(month.toString(), 15_000), MonthlyBudget(previous.toString(), 7_000)),
        )
        assertEquals(15_000L, data.spending(month, today).budget)
        assertEquals(2_500L, data.spending(month, today).remaining)
        assertEquals(7_000L, data.spending(previous, today).remaining)
        assertNull(data.spending(previous.minusMonths(1), today).remaining)
    }

    @Test
    fun zeroBudgetRemainsDistinctFromAnUnsetBudget() {
        val data = ledger(purchase).copy(budgets = listOf(MonthlyBudget(month.toString(), 0)))
        assertEquals(0L, data.spending(month, today).budget)
        assertEquals(-12_500L, data.spending(month, today).remaining)
        assertNull(ledger(purchase).spending(month, today).remaining)
    }

    @Test
    fun monthlyLimitChangesAndRemovalsPreserveHistory() {
        val original = month.minusMonths(1)
        val data = ledger().copy(
            limits = listOf(
                MonthlyLimit(groceries.id, original.toString(), 20_000),
                MonthlyLimit(groceries.id, month.plusMonths(1).toString(), 0),
            ),
        )
        assertEquals(20_000L, data.limitsFor(original)[groceries.id])
        assertEquals(20_000L, data.limitsFor(month)[groceries.id])
        assertNull(data.limitsFor(month.plusMonths(1))[groceries.id])
        assertNull(data.limitsFor(original.minusMonths(1))[groceries.id])
    }

    @Test
    fun amountsUseExactCurrencyUnitsAndLocalizedDecimals() {
        val us = Money("USD", Locale.US)
        val de = Money("EUR", Locale.GERMANY)
        assertEquals(10L, us.parse("0.10"))
        assertEquals(20L, us.parse("0.20"))
        assertEquals(30L, us.parse("0.30"))
        assertEquals(12_345L, de.parse("123,45"))
        assertEquals(12_345L, de.parse("123.45"))
        assertEquals("123,45", de.input(12_345))
        assertEquals(-123L, us.parse("−1.23"))
        assertEquals(123L, Money("JPY", Locale.JAPAN).parse("123"))
        assertNull(Money("JPY", Locale.JAPAN).parse("1.23"))
        assertNull(us.parse("1.001"))
        assertNull(us.parse("1e4"))
        assertNull(us.parse("10000000000.00"))
    }

    @Test
    fun entryValidationRejectsInvalidLinks() {
        val data = ledger()
        assertThrows(IllegalArgumentException::class.java) {
            validateEntry(payment.copy(destinationId = bank.id), data.accountsById, data.categoriesById)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateEntry(purchase.copy(categoryId = salary.id), data.accountsById, data.categoriesById)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateEntry(payment.copy(destinationId = 999), data.accountsById, data.categoriesById)
        }
    }

    @Test
    fun csvKeepsTransfersAndTextSafeForSpreadsheets() {
        val writer = StringWriter()
        writeLedgerCsv(ledger(purchase.copy(payee = "Store, \"City\"", note = "=10+5"), payment), writer)
        val csv = writer.toString()
        assertTrue(csv.contains("Expense,-125.00,PHP,\"Card\",\"\",\"Groceries\",\"Store, \"\"City\"\"\",\"'=10+5\"\r\n"))
        assertTrue(csv.contains("Transfer,-125.00,PHP,\"Bank\",\"Card\""))
        assertEquals(3, csv.trim().lines().size)
    }

    private fun ledger(vararg entries: LedgerEntry) =
        LedgerSnapshot(LedgerConfig("PHP"), listOf(bank, card), listOf(groceries, salary), entries.toList(), emptyList())
}
