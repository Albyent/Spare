package app.spareledger.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "ledger_config")
data class LedgerConfig(
    val currencyCode: String,
    @PrimaryKey val id: Int = 1,
)

@Entity(tableName = "accounts")
data class Account(
    val name: String,
    val kind: AccountKind,
    val openingMinor: Long,
    val archived: Boolean = false,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(tableName = "categories")
data class Category(
    val name: String,
    val kind: CategoryKind,
    val glyph: Glyph,
    val archived: Boolean = false,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"]),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["destinationId"]),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"]),
    ],
    indices = [Index("accountId"), Index("destinationId"), Index("categoryId"), Index("day")],
)
data class LedgerEntry(
    val kind: EntryKind,
    val minor: Long,
    val day: Long,
    val accountId: Long,
    val destinationId: Long? = null,
    val categoryId: Long? = null,
    val payee: String = "",
    val note: String = "",
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(
    tableName = "monthly_limits",
    primaryKeys = ["categoryId", "month"],
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"]),
    ],
)
data class MonthlyLimit(
    val categoryId: Long,
    val month: String,
    val minor: Long,
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_config WHERE id = 1")
    suspend fun config(): LedgerConfig?

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun accounts(): List<Account>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun categories(): List<Category>

    @Query("SELECT * FROM entries ORDER BY day DESC, id DESC")
    suspend fun entries(): List<LedgerEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun entry(id: Long): LedgerEntry?

    @Query("SELECT * FROM monthly_limits")
    suspend fun limits(): List<MonthlyLimit>

    @Insert suspend fun insertConfig(config: LedgerConfig)
    @Upsert suspend fun putAccount(account: Account)
    @Upsert suspend fun putCategory(category: Category)
    @Upsert suspend fun putEntry(entry: LedgerEntry)
    @Upsert suspend fun putLimit(limit: MonthlyLimit)

    @Insert suspend fun insertAccounts(accounts: List<Account>)
    @Insert suspend fun insertCategories(categories: List<Category>)
    @Insert suspend fun insertEntries(entries: List<LedgerEntry>)
    @Insert suspend fun insertLimits(limits: List<MonthlyLimit>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM monthly_limits") suspend fun clearLimits()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM ledger_config") suspend fun clearConfig()
}

@Database(
    entities = [LedgerConfig::class, Account::class, Category::class, LedgerEntry::class, MonthlyLimit::class],
    version = 1,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile private var instance: LedgerDatabase? = null

        fun get(context: Context): LedgerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LedgerDatabase::class.java,
                "spare.db",
            ).build().also { instance = it }
        }
    }
}
