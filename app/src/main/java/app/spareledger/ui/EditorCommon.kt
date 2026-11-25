package app.spareledger.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.spareledger.data.Account
import app.spareledger.data.AccountKind
import app.spareledger.data.MAX_NAME_LENGTH
import app.spareledger.data.Money
import app.spareledger.data.supportedYears
import java.io.Serializable
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorSheet(busy: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    val busyNow by rememberUpdatedState(busy)
    ModalBottomSheet(
        onDismissRequest = { if (!busyNow) onClose() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !busyNow || it != SheetValue.Hidden },
        ),
        containerColor = MaterialTheme.colorScheme.background,
        sheetMaxWidth = screenWidth,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { content() }
    }
}

@Composable
internal fun EditorFrame(
    title: String,
    busy: Boolean,
    error: String?,
    action: String,
    canSave: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    Column(Modifier.widthIn(max = screenWidth).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = screenGutter, end = 9.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            IconButton(onClick = onClose, enabled = !busy) {
                Icon(Icons.Outlined.Close, contentDescription = "Close")
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = screenGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = screenGutter, vertical = 12.dp)) {
            if (error != null) {
                Text(
                    error, Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    onSave()
                },
                enabled = canSave && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 51.dp),
            ) { Text(if (busy) "Saving…" else action) }
        }
    }
}

@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T?,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    badge: (@Composable (T) -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (options.isEmpty()) {
                Text("No choices available.")
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 410.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(options) { item ->
                        ChoiceRow(label(item), item == selected, onClick = { onSelect(item) }) {
                            badge?.invoke(item)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ChoiceRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (selected) Icon(Icons.Outlined.Check, "Selected")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryDateDialog(day: Long, today: LocalDate, onClose: () -> Unit, onDate: (Long) -> Unit) {
    val last = today.toEpochDay()
    val dateRules = remember(last) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Math.floorDiv(utcTimeMillis, 86_400_000L) <= last

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = day.coerceAtMost(last) * 86_400_000L,
        yearRange = supportedYears,
        selectableDates = dateRules,
    )
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(
                onClick = { picker.selectedDateMillis?.let { onDate(Math.floorDiv(it, 86_400_000L)) } },
                enabled = picker.selectedDateMillis != null,
            ) { Text("Use date") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    ) { DatePicker(picker) }
}

@Composable
internal fun AmountField(
    label: String,
    value: String,
    money: Money,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    large: Boolean = false,
    optional: Boolean = false,
    hint: String? = null,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val invalid = value.isNotBlank() && money.parse(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 25) onValue(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
        ),
        placeholder = if (optional || large) {
            { Text("0", style = MaterialTheme.typography.headlineSmall) }
        } else null,
        prefix = { Text(money.symbol) },
        isError = invalid,
        supportingText = if (invalid || hint != null) {
            { Text(if (invalid) "Enter a valid amount in " + money.code + "." else hint.orEmpty()) }
        } else null,
        singleLine = true,
        textStyle = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
        shape = MaterialTheme.shapes.small,
    )
}

internal data class AccountDraft(
    val name: String = "Cash",
    val kind: AccountKind = AccountKind.CASH,
    val opening: String = "",
) : Serializable {
    fun account(money: Money, original: Account? = null): Account? {
        val amount = if (opening.isBlank()) 0 else money.parse(opening) ?: return null
        if (name.isBlank()) return null
        val balance = if (kind == AccountKind.CARD) -amount else amount
        return Account(name.trim(), kind, balance, original?.archived ?: false, original?.id ?: 0)
    }
}

@Composable
internal fun AccountFields(draft: AccountDraft, money: Money, onChange: (AccountDraft) -> Unit) {
    Text("Account type", style = MaterialTheme.typography.titleSmall)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        for (kind in AccountKind.entries) {
            FilterChip(
                selected = draft.kind == kind,
                onClick = {
                    val name = if (draft.name == draft.kind.label) kind.label else draft.name
                    onChange(draft.copy(name = name, kind = kind))
                },
                label = { Text(kind.label) },
            )
        }
    }
    OutlinedTextField(
        draft.name, { if (it.length <= MAX_NAME_LENGTH) onChange(draft.copy(name = it)) },
        Modifier.fillMaxWidth(), label = { Text("Account name") },
        singleLine = true, shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    AmountField(
        if (draft.kind == AccountKind.CARD) "Opening amount owed" else "Opening balance",
        draft.opening, money, { onChange(draft.copy(opening = it)) }, optional = true,
    )
}

@Composable
internal fun EditorOption(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    TextButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 7.dp))
        Text(label)
    }
}
