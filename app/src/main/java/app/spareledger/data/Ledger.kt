package app.spareledger.data

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

enum class AccountKind(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    WALLET("E-wallet"),
    CARD("Credit card"),
}

enum class CategoryKind(val label: String) {
    EXPENSE("Expenses"),
    INCOME("Income"),
}

enum class EntryKind(val label: String, val categoryKind: CategoryKind?) {
    EXPENSE("Expense", CategoryKind.EXPENSE),
    INCOME("Income", CategoryKind.INCOME),
    TRANSFER("Transfer", null),
    REFUND("Refund", CategoryKind.EXPENSE),
}

enum class Glyph(val label: String) {
    GROCERIES("Groceries"),
    DINING("Food and drink"),
    TRANSPORT("Transport"),
    HOME("Home"),
    BILLS("Bills"),
    SHOPPING("Shopping"),
    HEALTH("Health"),
    LEISURE("Leisure"),
    TRAVEL("Travel"),
    INCOME("Income"),
    OTHER("Other"),
}

internal val defaultCategories = listOf(
    Category("Groceries", CategoryKind.EXPENSE, Glyph.GROCERIES),
    Category("Dining", CategoryKind.EXPENSE, Glyph.DINING),
    Category("Transport", CategoryKind.EXPENSE, Glyph.TRANSPORT),
    Category("Home", CategoryKind.EXPENSE, Glyph.HOME),
    Category("Bills", CategoryKind.EXPENSE, Glyph.BILLS),
    Category("Shopping", CategoryKind.EXPENSE, Glyph.SHOPPING),
    Category("Health", CategoryKind.EXPENSE, Glyph.HEALTH),
    Category("Leisure", CategoryKind.EXPENSE, Glyph.LEISURE),
    Category("Other", CategoryKind.EXPENSE, Glyph.OTHER),
    Category("Salary", CategoryKind.INCOME, Glyph.INCOME),
    Category("Other income", CategoryKind.INCOME, Glyph.OTHER),
)

internal const val MAX_MINOR = 999_999_999_999L
internal const val MAX_NAME_LENGTH = 48
internal const val MAX_PAYEE_LENGTH = 100
internal const val MAX_NOTE_LENGTH = 500
internal val supportedYears = 1900..2200
internal val supportedDays = LocalDate.of(supportedYears.first, 1, 1).toEpochDay()..
    LocalDate.of(supportedYears.last, 12, 31).toEpochDay()

internal fun supportedCurrency(code: String): Currency? =
    runCatching { Currency.getInstance(code) }.getOrNull()?.takeIf { it.defaultFractionDigits in 0..4 }

internal fun defaultCurrency(locale: Locale = Locale.getDefault()): String =
    runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        ?.takeIf { supportedCurrency(it) != null } ?: "USD"

internal class Money(val code: String, private val locale: Locale = Locale.getDefault()) {
    private val currency = requireNotNull(supportedCurrency(code))
    val digits: Int = currency.defaultFractionDigits
    val symbol: String = currency.getSymbol(locale)
    private val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    private val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = this@Money.currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }

    fun format(minor: Long, signed: Boolean = false): String {
        val formatted = formatter.format(BigDecimal.valueOf(minor, digits))
        return if (signed && minor > 0) "+" + formatted else formatted
    }

    fun input(minor: Long): String =
        BigDecimal.valueOf(minor, digits).stripTrailingZeros().toPlainString().replace('.', separator)

    fun parse(value: String): Long? {
        val text = value.trim().replace('−', '-')
        if (text.isEmpty() || text.length > 25) return null
        val decimal = if (separator != '.' && separator in text) separator else '.'
        val normalized = buildString {
            for (char in text) {
                val digit = Character.digit(char, 10)
                when {
                    digit >= 0 -> append(digit)
                    char == decimal -> append('.')
                    char == '-' && isEmpty() -> append('-')
                    else -> return null
                }
            }
        }
        if (normalized.substringAfter('.', "").length > digits) return null
        return runCatching { BigDecimal(normalized).movePointRight(digits).longValueExact() }
            .getOrNull()?.takeIf { it in -MAX_MINOR..MAX_MINOR }
    }
}

data class LedgerSnapshot(
    val config: LedgerConfig?,
    val accounts: List<Account>,
    val categories: List<Category>,
    val entries: List<LedgerEntry>,
    val limits: List<MonthlyLimit>,
    val budgets: List<MonthlyBudget> = emptyList(),
) {
    val accountsById = accounts.associateBy(Account::id)
    val categoriesById = categories.associateBy(Category::id)
    val activeAccounts = accounts.filterNot(Account::archived)
    val defaultAccountId: Long? by lazy {
        entries.asSequence()
            .filter { accountsById[it.accountId]?.archived == false }
            .maxByOrNull(LedgerEntry::id)?.accountId ?: activeAccounts.firstOrNull()?.id
    }
}

internal data class Spending(
    val budget: Long?,
    val spent: Long,
    val categories: Map<Long, Long>,
    val limits: Map<Long, Long>,
) {
    val remaining = budget?.minus(spent)
}

internal fun LedgerSnapshot.budgetFor(month: YearMonth): Long? =
    budgets.firstOrNull { it.month == month.toString() }?.minor

internal fun LedgerSnapshot.spending(month: YearMonth, today: LocalDate): Spending {
    val first = month.atDay(1).toEpochDay()
    val last = minOf(month.atEndOfMonth(), today).toEpochDay()
    var spent = 0L
    val totals = mutableMapOf<Long, Long>()

    for (entry in entries) {
        if (entry.day !in first..last) continue
        when (entry.kind) {
            EntryKind.EXPENSE, EntryKind.REFUND -> {
                val debit = if (entry.kind == EntryKind.EXPENSE) entry.minor else -entry.minor
                val category = requireNotNull(entry.categoryId)
                spent += debit
                totals[category] = totals.getOrDefault(category, 0) + debit
            }
            EntryKind.INCOME, EntryKind.TRANSFER -> Unit
        }
    }
    return Spending(budgetFor(month), spent, totals, limitsFor(month))
}

internal fun LedgerSnapshot.limitsFor(month: YearMonth): Map<Long, Long> {
    val key = month.toString()
    return limits.asSequence()
        .filter { it.month <= key }
        .groupBy(MonthlyLimit::categoryId)
        .mapValues { (_, rules) -> rules.maxBy(MonthlyLimit::month).minor }
        .filterValues { it > 0 }
}

internal fun LedgerSnapshot.balances(today: LocalDate): Map<Long, Long> {
    val totals = accounts.associate { it.id to it.openingMinor }.toMutableMap()
    for (entry in entries) {
        if (entry.day > today.toEpochDay()) continue
        totals[entry.accountId] = totals.getValue(entry.accountId) + entry.sourceDelta()
        entry.destinationId?.let { id -> totals[id] = totals.getValue(id) + entry.minor }
    }
    return totals
}

internal fun LedgerEntry.sourceDelta(): Long = when (kind) {
    EntryKind.EXPENSE, EntryKind.TRANSFER -> -minor
    EntryKind.INCOME, EntryKind.REFUND -> minor
}

internal fun parseMonth(key: String): YearMonth? =
    runCatching { YearMonth.parse(key) }.getOrNull()?.takeIf {
        it.year in supportedYears && it.toString() == key
    }
