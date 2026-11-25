package app.spareledger.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DonutSmall
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.spareledger.Appearance
import app.spareledger.SpareViewModel
import app.spareledger.data.CategoryKind
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.Money
import app.spareledger.data.budgetFor
import app.spareledger.data.limitsFor
import app.spareledger.data.parseMonth
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.YearMonth

private enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.DonutSmall),
    ACTIVITY("Activity", Icons.AutoMirrored.Outlined.ReceiptLong),
    ACCOUNTS("Accounts", Icons.Outlined.AccountBalanceWallet),
}

private enum class Page(val title: String) {
    SETTINGS("Settings"), CATEGORIES("Categories"), LIMITS("Monthly limits"),
}

private enum class SheetType { ENTRY, ACCOUNT, CATEGORY, LIMIT, BUDGET }

private data class SheetRequest(
    val type: SheetType,
    val id: Long? = null,
    val group: CategoryKind = CategoryKind.EXPENSE,
    val month: String? = null,
) : Serializable

@Composable
internal fun SpareApp(model: SpareViewModel, appearance: Appearance) {
    val data by model.data.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val loadFailed by model.loadFailed.collectAsStateWithLifecycle()
    val pendingBackup by model.pendingBackup.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    var page by rememberSaveable { mutableStateOf<Page?>(null) }
    var returnTab by rememberSaveable { mutableStateOf<Tab?>(null) }
    var limitsFromSettings by rememberSaveable { mutableStateOf(false) }
    var monthKey by rememberSaveable { mutableStateOf<String?>(null) }
    var limitKey by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(ActivityFilter()) }
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    var editorError by rememberSaveable { mutableStateOf<String?>(null) }
    val currentMonth = YearMonth.from(today)
    val overviewMonth = monthKey?.let(::parseMonth)?.coerceAtMost(currentMonth) ?: currentMonth
    val limitMonth = limitKey?.let(::parseMonth)?.coerceAtMost(currentMonth) ?: currentMonth
    val currency = data?.config?.currencyCode
    val money = remember(currency) { currency?.let(::Money) }
    val snackbar = remember { SnackbarHostState() }
    val screenState = rememberSaveableStateHolder()

    fun closeSheet() {
        editorError = null
        sheet = null
    }

    fun showActivity(nextFilter: ActivityFilter) {
        closeSheet()
        if (tab != Tab.ACTIVITY) returnTab = tab
        filter = nextFilter
        tab = Tab.ACTIVITY
    }

    fun goBack() {
        when (page) {
            Page.CATEGORIES -> page = Page.SETTINGS
            Page.LIMITS -> page = if (limitsFromSettings) Page.SETTINGS else null
            Page.SETTINGS -> page = null
            null -> returnTab?.let { tab = it; returnTab = null }
        }
    }

    fun showLimits(fromSettings: Boolean) {
        limitsFromSettings = fromSettings
        limitKey = if (fromSettings) null else monthKey
        page = Page.LIMITS
    }

    fun editEntry(entry: LedgerEntry) {
        editorError = null
        sheet = SheetRequest(SheetType.ENTRY, entry.id)
    }

    BackHandler(enabled = page != null || returnTab != null, onBack = ::goBack)
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) model.exportBackup(it)
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) model.exportCsv(it)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) model.previewBackup(it)
    }
    fun pickBackup() = restorePicker.launch(arrayOf("*/*"))

    LaunchedEffect(model) {
        model.messages.collect { event ->
            if (event.closeSheet) closeSheet()
            if (event.restored) {
                tab = Tab.OVERVIEW
                page = null
                returnTab = null
                monthKey = null
                limitKey = null
                filter = ActivityFilter()
            }
            val inline = event.error && (sheet != null || (data?.config == null && page == null && !loadFailed))
            if (inline) {
                editorError = event.message
            } else {
                snackbar.currentSnackbarData?.dismiss()
                launch { snackbar.showSnackbar(event.message) }
            }
        }
    }

    val mainScreen = currency != null && page == null
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        topBar = {
            val title = page?.title ?: tab.label.takeIf { mainScreen && tab != Tab.OVERVIEW } ?: "Spare"
            SpareHeader(
                title = title,
                onBack = if (page != null) ::goBack else null,
                onSettings = if (page == null && data != null) {
                    { editorError = null; page = Page.SETTINGS }
                } else null,
            )
        },
        bottomBar = {
            if (mainScreen) {
                val needsAccount = data?.activeAccounts.isNullOrEmpty()
                SpareNavigation(
                    tab, needsAccount, busy,
                    onTab = { item ->
                        if (tab != item) returnTab = null
                        tab = item
                    },
                    onAdd = {
                        editorError = null
                        sheet = SheetRequest(if (needsAccount) SheetType.ACCOUNT else SheetType.ENTRY)
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val snapshot = data
            val subPage = page
            when {
                loadFailed -> Box(Modifier.padding(screenGutter)) {
                    EmptyPanel(Icons.Outlined.FolderOff, "Couldn’t open your ledger", action = "Try again", onAction = model::load)
                }
                snapshot == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                subPage == Page.SETTINGS -> SettingsScreen(
                    appearance.mode, appearance.color, currency, busy, model::changeTheme, model::changeThemeColor,
                    onCategories = { page = Page.CATEGORIES }, onLimits = { showLimits(true) },
                    onBackup = { backupPicker.launch("Spare-backup-$today.json") }, onRestore = ::pickBackup,
                    onCsv = { csvPicker.launch("Spare-transactions-$today.csv") },
                )
                money == null -> screenState.SaveableStateProvider("setup") {
                    SetupScreen(busy, editorError, model::setup, ::pickBackup)
                }
                subPage == Page.CATEGORIES -> screenState.SaveableStateProvider(subPage.name) {
                    CategoriesScreen(
                        snapshot, onAdd = { sheet = SheetRequest(SheetType.CATEGORY, group = it) },
                        onEdit = { sheet = SheetRequest(SheetType.CATEGORY, it.id, it.kind) },
                    )
                }
                subPage == Page.LIMITS -> LimitsScreen(
                    snapshot, limitMonth, currentMonth, money, onMonth = { limitKey = it.toString() },
                    onEdit = { sheet = SheetRequest(SheetType.LIMIT, it.id) },
                )
                else -> screenState.SaveableStateProvider(tab.name) {
                    when (tab) {
                        Tab.OVERVIEW -> Overview(
                            snapshot, today, overviewMonth, money, appearance.cardAmountsHidden,
                            onMonth = { monthKey = if (it == currentMonth) null else it.toString() },
                            onCategory = { showActivity(ActivityFilter(month = overviewMonth.toString(), categoryId = it)) },
                            onActivity = { showActivity(ActivityFilter(month = overviewMonth.toString())) },
                            onEntry = ::editEntry, onLimits = { showLimits(false) },
                            onBudget = { sheet = SheetRequest(SheetType.BUDGET, month = overviewMonth.toString()) },
                            onToggleCardAmounts = model::toggleCardAmounts,
                        )
                        Tab.ACTIVITY -> ActivityScreen(snapshot, money, today, filter, { filter = it }, ::editEntry)
                        Tab.ACCOUNTS -> AccountsScreen(
                            snapshot, today, money, { sheet = SheetRequest(SheetType.ACCOUNT) },
                            { sheet = SheetRequest(SheetType.ACCOUNT, it.id) },
                        )
                    }
                }
            }
        }
    }

    val editor = sheet
    val snapshot = data
    if (editor != null && snapshot != null && money != null) {
        key(editor) {
            EditorSheet(busy, ::closeSheet) {
                when (editor.type) {
                    SheetType.ENTRY -> EntryEditor(
                        snapshot, snapshot.entries.firstOrNull { it.id == editor.id }, money, today,
                        initialAccount = filter.accountId.takeIf { tab == Tab.ACTIVITY },
                        initialCategory = filter.categoryId.takeIf { tab == Tab.ACTIVITY },
                        busy = busy, error = editorError, onClose = ::closeSheet,
                        onSave = model::saveEntry, onRemove = model::removeEntry,
                    )
                    SheetType.ACCOUNT -> AccountEditor(
                        snapshot.accountsById[editor.id], money, busy, editorError, ::closeSheet,
                        model::saveAccount, model::changeAccountArchive, { showActivity(ActivityFilter(accountId = it)) },
                    )
                    SheetType.CATEGORY -> CategoryEditor(
                        snapshot.categoriesById[editor.id], editor.group, busy, editorError, ::closeSheet,
                        model::saveCategory, model::changeCategoryArchive,
                    )
                    SheetType.LIMIT -> snapshot.categoriesById[editor.id]?.let { category ->
                        LimitEditor(
                            category, limitMonth, snapshot.limitsFor(limitMonth)[category.id], money,
                            busy, editorError, ::closeSheet, model::saveLimit,
                        )
                    }
                    SheetType.BUDGET -> editor.month?.let(::parseMonth)?.let { month ->
                        BudgetEditor(
                            month, snapshot.budgetFor(month), money, busy, editorError, ::closeSheet,
                            onSave = { model.saveBudget(month.toString(), it) },
                        )
                    }
                }
            }
        }
    }
    pendingBackup?.let { backup ->
        RestoreDialog(backup, currency != null, busy, model::dismissBackup, model::restore)
    }
}

@Composable
private fun SpareNavigation(
    tab: Tab,
    needsAccount: Boolean,
    busy: Boolean,
    onTab: (Tab) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NavigationBar(
                Modifier.widthIn(max = screenWidth),
                containerColor = colors.surface,
                tonalElevation = 0.dp,
            ) {
                for (item in Tab.entries) {
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { onTab(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
                Box(
                    Modifier.weight(1f).align(Alignment.CenterVertically).padding(end = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onAdd, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = if (needsAccount) "Add account" else "Add transaction",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreDialog(
    backup: LedgerSnapshot,
    replacing: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    val accountCount = backup.accounts.size
    val entryCount = backup.entries.size
    val description = "$accountCount " + if (accountCount == 1) "account" else "accounts"
    val entries = "$entryCount " + if (entryCount == 1) "transaction" else "transactions"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore backup?") },
        text = {
            Text(
                description + " and " + entries + " in " + backup.config?.currencyCode + "." +
                    if (replacing) " This will replace your current ledger." else "",
            )
        },
        confirmButton = { TextButton(onClick = onRestore, enabled = !busy) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
