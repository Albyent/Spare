package app.spareledger.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.spareledger.data.Account
import app.spareledger.data.Category
import app.spareledger.data.CategoryKind
import app.spareledger.data.EntryKind
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.MAX_NOTE_LENGTH
import app.spareledger.data.MAX_PAYEE_LENGTH
import app.spareledger.data.Money
import kotlinx.coroutines.delay
import java.time.LocalDate

private enum class EntryPicker { ACCOUNT, DESTINATION, CATEGORY, DATE, DELETE }

@Composable
internal fun EntryEditor(
    data: LedgerSnapshot,
    entry: LedgerEntry?,
    money: Money,
    today: LocalDate,
    initialAccount: Long?,
    initialCategory: Long?,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (LedgerEntry) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val preselected = data.categoriesById[initialCategory]?.takeUnless(Category::archived)
    val initialKind = if (preselected?.kind == CategoryKind.INCOME) EntryKind.INCOME else EntryKind.EXPENSE
    var kind by rememberSaveable { mutableStateOf(entry?.kind ?: initialKind) }
    var amount by rememberSaveable { mutableStateOf(entry?.let { money.input(it.minor) }.orEmpty()) }
    var accountId by rememberSaveable {
        mutableStateOf(entry?.accountId ?: initialAccount?.takeIf { data.accountsById[it]?.archived == false } ?: data.defaultAccountId)
    }
    var destinationId by rememberSaveable { mutableStateOf(entry?.destinationId) }
    var categoryId by rememberSaveable { mutableStateOf(entry?.categoryId ?: preselected?.id) }
    var day by rememberSaveable { mutableStateOf(entry?.day ?: today.toEpochDay()) }
    var payee by rememberSaveable { mutableStateOf(entry?.payee.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(entry?.note.orEmpty()) }
    var showDetails by rememberSaveable { mutableStateOf(payee.isNotBlank() || note.isNotBlank()) }
    var picker by rememberSaveable { mutableStateOf<EntryPicker?>(null) }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (entry == null && !focusedOnce) {
            delay(280)
            amountFocus.requestFocus()
            keyboard?.show()
            focusedOnce = true
        }
    }

    val transfer = kind == EntryKind.TRANSFER
    val category = data.categoriesById[categoryId]?.takeIf { it.kind == kind.categoryKind }
    val source = data.accountsById[accountId]
    val destination = data.accountsById[destinationId]?.takeIf { it.id != accountId }
    val minor = money.parse(amount)?.takeIf { it > 0 }
    val validDay = day <= today.toEpochDay()
    val draft = if (source != null && minor != null && validDay &&
        (if (transfer) destination != null else category != null)
    ) {
        LedgerEntry(
            kind, minor, day, source.id, destination?.id.takeIf { transfer }, category?.id.takeUnless { transfer },
            payee.trim().takeUnless { transfer }.orEmpty(), note.trim(), entry?.id ?: 0,
        )
    } else null

    fun openPicker(choice: EntryPicker) {
        focus.clearFocus()
        keyboard?.hide()
        picker = choice
    }

    EditorFrame(
        title = if (entry == null) "Add transaction" else "Edit transaction",
        busy = busy, error = error, action = if (entry == null) "Save transaction" else "Save changes",
        canSave = draft != null, onClose = onClose, onSave = { draft?.let(onSave) },
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EntryKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = {
                        if (kind.categoryKind != option.categoryKind) categoryId = null
                        kind = option
                    },
                    label = { Text(option.label) },
                )
            }
        }
        AmountField(
            "Amount", amount, money, { amount = it },
            focusRequester = amountFocus, large = true,
            hint = "Enter an amount above zero.".takeIf { money.parse(amount)?.let { it <= 0 } == true },
        )
        if (transfer) {
            PickField("From account", source?.name ?: "Choose account", { openPicker(EntryPicker.ACCOUNT) })
            PickField("To account", destination?.name ?: "Choose account", { openPicker(EntryPicker.DESTINATION) })
            if (data.activeAccounts.size < 2 && destination == null) {
                Text(
                    "Add a second account from Accounts to record transfers.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            PickField("Category", category?.name ?: "Choose category", { openPicker(EntryPicker.CATEGORY) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (!transfer) {
                Box(Modifier.weight(1f)) {
                    PickField("Account", source?.name ?: "Choose account", { openPicker(EntryPicker.ACCOUNT) })
                }
            }
            Box(Modifier.weight(1f)) {
                val current = LocalDate.ofEpochDay(day)
                PickField("Date", if (current == today) "Today" else shortDate(current), { openPicker(EntryPicker.DATE) })
            }
        }
        if (!validDay) {
            Text("Choose today or an earlier date.", color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = { showDetails = !showDetails }) {
            Text(
                when {
                    showDetails -> "Hide details"
                    transfer -> "Add a note"
                    else -> "Add payee or note"
                },
            )
        }
        if (showDetails) {
            if (!transfer) {
                OutlinedTextField(
                    payee, { if (it.length <= MAX_PAYEE_LENGTH) payee = it },
                    Modifier.fillMaxWidth(), label = { Text("Payee") }, singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
            }
            OutlinedTextField(
                note, { if (it.length <= MAX_NOTE_LENGTH) note = it }, Modifier.fillMaxWidth(),
                label = { Text("Note") }, maxLines = 3, shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        if (entry != null) {
            EditorOption("Delete transaction", Icons.Outlined.DeleteOutline, !busy) { openPicker(EntryPicker.DELETE) }
        }
    }

    when (picker) {
        EntryPicker.ACCOUNT -> ChoiceDialog(
            if (transfer) "From account" else "Choose account",
            data.accounts.filter { !it.archived || it.id == entry?.accountId }, Account::name,
            source, { picker = null }, {
                accountId = it.id
                if (destinationId == it.id) destinationId = null
                picker = null
            },
            badge = { IconBadge(it.kind.icon) },
        )
        EntryPicker.DESTINATION -> ChoiceDialog(
            "To account", data.accounts.filter { it.id != accountId && (!it.archived || it.id == entry?.destinationId) },
            Account::name, destination, { picker = null }, { destinationId = it.id; picker = null },
            badge = { IconBadge(it.kind.icon) },
        )
        EntryPicker.CATEGORY -> {
            val choices = remember(data, kind, entry) {
                val usage = data.entries.filter { it.categoryId != null }.groupBy { it.categoryId }
                    .mapValues { (_, entries) -> entries.maxOf { it.id } }
                data.categories.filter { it.kind == kind.categoryKind && (!it.archived || it.id == entry?.categoryId) }
                    .sortedWith(compareByDescending<Category> { usage[it.id] ?: 0L }.thenBy(Category::id))
            }
            ChoiceDialog(
                "Choose category", choices, Category::name, category, { picker = null },
                { categoryId = it.id; picker = null }, badge = { CategoryBadge(it.glyph) },
            )
        }
        EntryPicker.DATE -> EntryDateDialog(day, today, { picker = null }, { day = it; picker = null })
        EntryPicker.DELETE -> if (entry != null) {
            AlertDialog(
                onDismissRequest = { picker = null },
                title = { Text("Delete transaction?") },
                text = { Text("Its amount will be removed from your account balances and reports.") },
                confirmButton = {
                    TextButton(onClick = { picker = null; onRemove(entry.id) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancel") } },
            )
        }
        null -> Unit
    }
}
