package app.spareledger

import android.app.Application
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.spareledger.data.Account
import app.spareledger.data.Category
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerInput
import app.spareledger.data.LedgerRepository
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.LocalFiles
import app.spareledger.data.MonthlyLimit
import app.spareledger.ui.SpareApp
import app.spareledger.ui.SpareTheme
import app.spareledger.ui.ThemeColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var model: SpareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        model = ViewModelProvider(this)[SpareViewModel::class.java]
        setContent {
            val appearance by model.appearance.collectAsStateWithLifecycle()
            val dark = when (appearance.mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            SpareTheme(dark, appearance.color) { SpareApp(model, appearance) }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    model.refreshDate()
                    delay(30_000)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refreshDate()
    }
}

internal enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark"),
}

internal data class Appearance(
    val mode: ThemeMode,
    val color: ThemeColor,
    val cardAmountsHidden: Boolean,
)

internal data class Feedback(
    val message: String,
    val closeSheet: Boolean = false,
    val restored: Boolean = false,
    val error: Boolean = false,
)

class SpareViewModel(private val application: Application) : AndroidViewModel(application) {
    private val ledger = LedgerRepository(application)
    private val preferences = application.getSharedPreferences("appearance", 0)
    private val currentAppearance = MutableStateFlow(
        Appearance(
            mode = ThemeMode.entries.firstOrNull { it.name == preferences.getString("theme", null) }
                ?: ThemeMode.SYSTEM,
            color = ThemeColor.entries.firstOrNull { it.name == preferences.getString("theme_color", null) }
                ?: ThemeColor.FOREST,
            cardAmountsHidden = preferences.getBoolean("hide_card_amounts", false),
        ),
    )
    internal val appearance = currentAppearance.asStateFlow()

    private val currentData = MutableStateFlow<LedgerSnapshot?>(null)
    internal val data = currentData.asStateFlow()
    private val failedLoad = MutableStateFlow(false)
    internal val loadFailed = failedLoad.asStateFlow()
    private val operation = MutableStateFlow(false)
    internal val busy = operation.asStateFlow()
    private val currentDate = MutableStateFlow(LocalDate.now())
    internal val today = currentDate.asStateFlow()
    private val selectedBackup = MutableStateFlow<LedgerSnapshot?>(null)
    internal val pendingBackup = selectedBackup.asStateFlow()
    private val feedback = Channel<Feedback>(Channel.BUFFERED)
    internal val messages = feedback.receiveAsFlow()

    init {
        load()
    }

    internal fun load() {
        failedLoad.value = false
        execute("Couldn’t open the ledger.", onFailure = { failedLoad.value = true }) {
            currentData.value = ledger.load()
        }
    }

    internal fun refreshDate() {
        currentDate.value = LocalDate.now()
    }

    internal fun changeTheme(mode: ThemeMode) = saveAppearance(currentAppearance.value.copy(mode = mode))

    internal fun changeThemeColor(color: ThemeColor) = saveAppearance(currentAppearance.value.copy(color = color))

    internal fun toggleCardAmounts() {
        val appearance = currentAppearance.value
        saveAppearance(appearance.copy(cardAmountsHidden = !appearance.cardAmountsHidden))
    }

    private fun saveAppearance(next: Appearance) {
        preferences.edit {
            putString("theme", next.mode.name)
            putString("theme_color", next.color.name)
            putBoolean("hide_card_amounts", next.cardAmountsHidden)
        }
        currentAppearance.value = next
    }

    internal fun setup(currencyCode: String, account: Account) =
        write("Couldn’t create the ledger.", null, closeSheet = false) { ledger.start(currencyCode, account) }

    internal fun saveAccount(account: Account) =
        write("Couldn’t save the account.", "Account saved") { ledger.saveAccount(account) }

    internal fun changeAccountArchive(account: Account) =
        write("Couldn’t update the account.", if (account.archived) "Account restored" else "Account archived") {
            ledger.archiveAccount(account.id, !account.archived)
        }

    internal fun saveCategory(category: Category) =
        write("Couldn’t save the category.", "Category saved") { ledger.saveCategory(category) }

    internal fun changeCategoryArchive(category: Category) =
        write("Couldn’t update the category.", if (category.archived) "Category restored" else "Category archived") {
            ledger.archiveCategory(category.id, !category.archived)
        }

    internal fun saveEntry(entry: LedgerEntry) =
        write("Couldn’t save the transaction.", "Transaction saved") { ledger.saveEntry(entry) }

    internal fun removeEntry(id: Long) =
        write("Couldn’t delete the transaction.", "Transaction deleted") { ledger.removeEntry(id) }

    internal fun saveLimit(limit: MonthlyLimit) =
        write("Couldn’t update the monthly limit.", if (limit.minor == 0L) "Limit removed" else "Limit saved") {
            ledger.saveLimit(limit)
        }

    internal fun saveBudget(month: String, amount: Long?) =
        write("Couldn’t update the monthly budget.", if (amount == null) "Budget removed" else "Budget saved") {
            ledger.saveBudget(month, amount)
        }

    internal fun exportBackup(uri: Uri) = execute("Couldn’t save the backup.") {
        LocalFiles.backup(application, uri, requireNotNull(currentData.value))
        feedback.send(Feedback("Backup saved"))
    }

    internal fun exportCsv(uri: Uri) = execute("Couldn’t export the transactions.") {
        LocalFiles.exportCsv(application, uri, requireNotNull(currentData.value))
        feedback.send(Feedback("CSV saved"))
    }

    internal fun previewBackup(uri: Uri) = execute("Couldn’t read a valid Spare backup from that file.") {
        selectedBackup.value = LocalFiles.readBackup(application, uri)
    }

    internal fun dismissBackup() {
        if (!operation.value) selectedBackup.value = null
    }

    internal fun restore() = execute("Couldn’t restore the backup.") {
        currentData.value = ledger.restore(requireNotNull(selectedBackup.value))
        selectedBackup.value = null
        feedback.send(Feedback("Backup restored", closeSheet = true, restored = true))
    }

    private fun write(
        failure: String,
        success: String?,
        closeSheet: Boolean = true,
        change: suspend () -> LedgerSnapshot,
    ) = execute(failure) {
        currentData.value = change()
        if (success != null) feedback.send(Feedback(success, closeSheet))
    }

    private fun execute(failure: String, onFailure: () -> Unit = {}, operation: suspend () -> Unit) {
        if (this.operation.value) return
        this.operation.value = true
        viewModelScope.launch {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onFailure()
                feedback.send(Feedback((error as? LedgerInput)?.message ?: failure, error = true))
            } finally {
                this@SpareViewModel.operation.value = false
            }
        }
    }
}
