package app.spareledger.data

import android.content.Context
import androidx.room.withTransaction

internal class LedgerInput(message: String) : IllegalArgumentException(message)

internal class LedgerRepository(context: Context) {
    private val database = LedgerDatabase.get(context)
    private val dao = database.ledgerDao()

    suspend fun load(): LedgerSnapshot = database.withTransaction { snapshot() }

    suspend fun start(currencyCode: String, account: Account): LedgerSnapshot = update {
        if (config() != null) throw LedgerInput("Your ledger is already set up.")
        require(supportedCurrency(currencyCode) != null)
        require(account.id == 0L && !account.archived)
        validateAccount(account)
        insertConfig(LedgerConfig(currencyCode))
        putAccount(account)
        insertCategories(defaultCategories)
    }

    suspend fun saveAccount(account: Account): LedgerSnapshot = update {
        validateAccount(account)
        val all = accounts()
        val previous = all.firstOrNull { it.id == account.id }
        require(account.id == 0L || previous != null)
        if (all.any { it.id != account.id && it.name.equals(account.name, ignoreCase = true) }) {
            throw LedgerInput("An account with that name already exists.")
        }
        putAccount(account.copy(archived = previous?.archived ?: false))
    }

    suspend fun archiveAccount(id: Long, archived: Boolean): LedgerSnapshot = update {
        val account = accounts().firstOrNull { it.id == id } ?: throw LedgerInput("Account unavailable.")
        putAccount(account.copy(archived = archived))
    }

    suspend fun saveCategory(category: Category): LedgerSnapshot = update {
        validateCategory(category)
        val all = categories()
        val previous = all.firstOrNull { it.id == category.id }
        require(category.id == 0L || previous?.kind == category.kind)
        if (all.any {
                it.id != category.id && it.kind == category.kind && it.name.equals(category.name, ignoreCase = true)
            }
        ) {
            throw LedgerInput("A category with that name already exists.")
        }
        putCategory(category.copy(archived = previous?.archived ?: false))
    }

    suspend fun archiveCategory(id: Long, archived: Boolean): LedgerSnapshot = update {
        val category = categories().firstOrNull { it.id == id } ?: throw LedgerInput("Category unavailable.")
        putCategory(category.copy(archived = archived))
    }

    suspend fun saveEntry(entry: LedgerEntry): LedgerSnapshot = update {
        val accountIndex = accounts().associateBy(Account::id)
        val categoryIndex = categories().associateBy(Category::id)
        val previous = if (entry.id == 0L) null else checkNotNull(entry(entry.id))
        validateEntry(entry, accountIndex, categoryIndex)
        if (accountIndex.getValue(entry.accountId).archived && entry.accountId != previous?.accountId) {
            throw LedgerInput("Choose an active account.")
        }
        entry.destinationId?.let { id ->
            if (accountIndex.getValue(id).archived && id != previous?.destinationId) {
                throw LedgerInput("Choose an active destination account.")
            }
        }
        entry.categoryId?.let { id ->
            if (categoryIndex.getValue(id).archived && id != previous?.categoryId) {
                throw LedgerInput("Choose an active category.")
            }
        }
        putEntry(entry)
    }

    suspend fun removeEntry(id: Long): LedgerSnapshot = update { deleteEntry(id) }

    suspend fun saveLimit(limit: MonthlyLimit): LedgerSnapshot = update {
        validateLimit(limit, categories().associateBy(Category::id))
        putLimit(limit)
    }

    suspend fun restore(data: LedgerSnapshot): LedgerSnapshot = update {
        clearEntries()
        clearLimits()
        clearAccounts()
        clearCategories()
        clearConfig()
        insertConfig(requireNotNull(data.config))
        insertAccounts(data.accounts)
        insertCategories(data.categories)
        insertEntries(data.entries)
        insertLimits(data.limits)
    }

    private suspend fun update(operation: suspend LedgerDao.() -> Unit): LedgerSnapshot =
        database.withTransaction {
            dao.operation()
            snapshot()
        }

    private suspend fun snapshot() =
        LedgerSnapshot(dao.config(), dao.accounts(), dao.categories(), dao.entries(), dao.limits())
}

internal fun validateAccount(account: Account) {
    require(account.name.isNotBlank() && account.name.length <= MAX_NAME_LENGTH)
    require(account.openingMinor in -MAX_MINOR..MAX_MINOR)
}

internal fun validateCategory(category: Category) {
    require(category.name.isNotBlank() && category.name.length <= MAX_NAME_LENGTH)
}

internal fun validateEntry(entry: LedgerEntry, accounts: Map<Long, Account>, categories: Map<Long, Category>) {
    require(entry.minor in 1..MAX_MINOR && entry.day in supportedDays)
    require(entry.accountId in accounts)
    require(entry.payee.length <= MAX_PAYEE_LENGTH && entry.note.length <= MAX_NOTE_LENGTH)
    when (val categoryKind = entry.kind.categoryKind) {
        null -> {
            require(entry.categoryId == null)
            require(entry.destinationId != entry.accountId && entry.destinationId in accounts)
        }
        else -> {
            require(entry.destinationId == null)
            require(categories[entry.categoryId]?.kind == categoryKind)
        }
    }
}

internal fun validateLimit(limit: MonthlyLimit, categories: Map<Long, Category>) {
    require(categories[limit.categoryId]?.kind == CategoryKind.EXPENSE)
    require(parseMonth(limit.month) != null)
    require(limit.minor in 0..MAX_MINOR)
}
