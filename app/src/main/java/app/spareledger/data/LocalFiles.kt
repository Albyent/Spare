package app.spareledger.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.Writer
import java.math.BigDecimal
import java.time.LocalDate

internal object LocalFiles {
    private const val FORMAT = "spare-ledger"
    private const val VERSION = 1
    private const val MAX_BYTES = 24 * 1024 * 1024

    suspend fun backup(context: Context, uri: Uri, data: LedgerSnapshot) = withContext(Dispatchers.IO) {
        val document = fields(
            "format" to FORMAT,
            "version" to VERSION,
            "currency" to requireNotNull(data.config).currencyCode,
            "accounts" to array(data.accounts) {
                fields(
                    "id" to it.id, "name" to it.name, "kind" to it.kind.name,
                    "openingMinor" to it.openingMinor, "archived" to it.archived,
                )
            },
            "categories" to array(data.categories) {
                fields(
                    "id" to it.id, "name" to it.name, "kind" to it.kind.name,
                    "glyph" to it.glyph.name, "archived" to it.archived,
                )
            },
            "entries" to array(data.entries) {
                fields(
                    "id" to it.id, "kind" to it.kind.name, "minor" to it.minor, "day" to it.day,
                    "accountId" to it.accountId, "destinationId" to it.destinationId,
                    "categoryId" to it.categoryId, "payee" to it.payee, "note" to it.note,
                )
            },
            "limits" to array(data.limits) {
                fields("categoryId" to it.categoryId, "month" to it.month, "minor" to it.minor)
            },
        )
        val bytes = document.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
    }

    suspend fun readBackup(context: Context, uri: Uri): LedgerSnapshot = withContext(Dispatchers.IO) {
        val text = checkNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count == -1) break
                require(output.size() + count <= MAX_BYTES)
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
        val parser = JSONTokener(text)
        val document = parser.nextValue() as JSONObject
        require(parser.nextClean() == '\u0000')
        require(document.text("format") == FORMAT && document.integer("version") == VERSION.toLong())
        val code = document.text("currency")
        require(supportedCurrency(code) != null)

        val accounts = document.list("accounts") {
            Account(
                name = text("name"), kind = AccountKind.valueOf(text("kind")),
                openingMinor = integer("openingMinor"), archived = boolean("archived"), id = id("id"),
            ).also(::validateAccount)
        }
        val categories = document.list("categories") {
            Category(
                name = text("name"), kind = CategoryKind.valueOf(text("kind")),
                glyph = Glyph.valueOf(text("glyph")), archived = boolean("archived"), id = id("id"),
            ).also(::validateCategory)
        }
        val accountIndex = accounts.associateBy(Account::id)
        val categoryIndex = categories.associateBy(Category::id)
        require(accountIndex.size == accounts.size && categoryIndex.size == categories.size)
        val entries = document.list("entries") {
            LedgerEntry(
                kind = EntryKind.valueOf(text("kind")), minor = integer("minor"), day = integer("day"),
                accountId = id("accountId"), destinationId = nullableId("destinationId"),
                categoryId = nullableId("categoryId"), payee = text("payee"), note = text("note"), id = id("id"),
            ).also { validateEntry(it, accountIndex, categoryIndex) }
        }
        require(entries.distinctBy(LedgerEntry::id).size == entries.size)
        val limits = document.list("limits") {
            MonthlyLimit(id("categoryId"), text("month"), integer("minor")).also {
                validateLimit(it, categoryIndex)
            }
        }
        require(limits.distinctBy { it.categoryId to it.month }.size == limits.size)
        LedgerSnapshot(LedgerConfig(code), accounts, categories, entries, limits)
    }

    suspend fun exportCsv(context: Context, uri: Uri, data: LedgerSnapshot) = withContext(Dispatchers.IO) {
        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use {
            it.write('\uFEFF'.code)
            writeLedgerCsv(data, it)
        }
    }

    private fun fields(vararg entries: Pair<String, Any?>) = JSONObject().apply {
        entries.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }

    private fun <T> array(values: List<T>, encode: (T) -> JSONObject) =
        JSONArray().apply { values.forEach { put(encode(it)) } }

    private fun <T> JSONObject.list(key: String, decode: JSONObject.() -> T): List<T> {
        val values = getJSONArray(key)
        return List(values.length()) { values.getJSONObject(it).decode() }
    }

    private fun JSONObject.text(key: String) = get(key) as String
    private fun JSONObject.boolean(key: String) = get(key) as Boolean

    private fun JSONObject.integer(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> error("Expected an integer")
    }

    private fun JSONObject.id(key: String) = integer(key).also { require(it in 1..Int.MAX_VALUE.toLong()) }

    private fun JSONObject.nullableId(key: String) = if (get(key) == JSONObject.NULL) null else id(key)
}

internal fun writeLedgerCsv(data: LedgerSnapshot, writer: Writer) {
    val code = requireNotNull(data.config).currencyCode
    val digits = requireNotNull(supportedCurrency(code)).defaultFractionDigits
    writer.append("date,type,amount,currency,account,destination,category,payee,note\r\n")
    for (entry in data.entries) {
        val row = listOf(
            LocalDate.ofEpochDay(entry.day).toString(),
            entry.kind.label,
            BigDecimal.valueOf(entry.sourceDelta(), digits).toPlainString(),
            code,
            csvText(data.accountsById.getValue(entry.accountId).name),
            csvText(data.accountsById[entry.destinationId]?.name.orEmpty()),
            csvText(data.categoriesById[entry.categoryId]?.name.orEmpty()),
            csvText(entry.payee),
            csvText(entry.note),
        )
        writer.append(row.joinToString(",")).append("\r\n")
    }
}

private fun csvText(value: String): String {
    val starter = value.trimStart(' ').firstOrNull()
    val safe = if (starter in listOf('=', '+', '-', '@', '\t', '\r', '\n')) "'" + value else value
    return "\"" + safe.replace("\"", "\"\"") + "\""
}
