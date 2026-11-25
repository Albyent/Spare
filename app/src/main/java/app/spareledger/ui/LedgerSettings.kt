package app.spareledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.spareledger.data.Category
import app.spareledger.data.CategoryKind
import app.spareledger.data.Glyph
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.MAX_NAME_LENGTH
import app.spareledger.data.Money
import app.spareledger.data.MonthlyLimit
import app.spareledger.data.limitsFor
import java.time.YearMonth

@Composable
internal fun CategoriesScreen(
    data: LedgerSnapshot,
    onAdd: (CategoryKind) -> Unit,
    onEdit: (Category) -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(CategoryKind.EXPENSE) }
    val categories = data.categories.filter { it.kind == kind }.sortedBy { it.name.lowercase() }
    val active = categories.filterNot(Category::archived)
    val archived = categories.filter(Category::archived)
    SettingsColumn {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option, onClick = { kind = option }, label = { Text(option.label) },
                )
            }
            TextButton(onClick = { onAdd(kind) }, modifier = Modifier.weight(1f)) {
                Text("New category", textAlign = TextAlign.Center)
            }
        }
        if (active.isEmpty()) {
            EmptyPanel(Icons.Outlined.Category, "No active categories")
        } else {
            CategoryRows(active, onClick = onEdit)
        }
        if (archived.isNotEmpty()) {
            Column {
                SectionTitle("Archived")
                CategoryRows(archived, onClick = onEdit)
            }
        }
    }
}

@Composable
private fun CategoryRows(
    categories: List<Category>,
    onClick: (Category) -> Unit,
    value: ((Category) -> String)? = null,
) {
    Panel {
        Column {
            categories.forEachIndexed { index, category ->
                if (index != 0) Rule()
                Surface(
                    onClick = { onClick(category) }, color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 61.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CategoryBadge(category.glyph)
                        Text(category.name, Modifier.weight(1.2f), style = MaterialTheme.typography.titleSmall)
                        if (value != null) {
                            AmountText(
                                value(category), Modifier.weight(1f), alignment = TextAlign.End,
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryEditor(
    category: Category?,
    kind: CategoryKind,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (Category) -> Unit,
    onArchive: (Category) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(category?.name.orEmpty()) }
    var glyph by rememberSaveable {
        mutableStateOf(category?.glyph ?: if (kind == CategoryKind.INCOME) Glyph.INCOME else Glyph.OTHER)
    }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    EditorFrame(
        title = if (category == null) "New category" else "Edit category",
        busy = busy, error = error, action = if (category == null) "Create category" else "Save changes",
        canSave = name.isNotBlank(), onClose = onClose, onSave = {
            onSave(Category(name.trim(), kind, glyph, category?.archived ?: false, category?.id ?: 0))
        },
    ) {
        Text(kind.label, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            name, { if (it.length <= MAX_NAME_LENGTH) name = it },
            Modifier.fillMaxWidth(), label = { Text("Category name") }, singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        PickField("Icon", glyph.label, {
            focus.clearFocus()
            keyboard?.hide()
            choosing = true
        })
        if (category != null) {
            EditorOption(
                if (category.archived) "Restore category" else "Archive category",
                if (category.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, !busy,
            ) { onArchive(category) }
        }
    }
    if (choosing) {
        ChoiceDialog(
            "Choose icon", Glyph.entries, Glyph::label, glyph, { choosing = false },
            { glyph = it; choosing = false }, badge = { CategoryBadge(it) },
        )
    }
}

@Composable
internal fun LimitsScreen(
    data: LedgerSnapshot,
    month: YearMonth,
    current: YearMonth,
    money: Money,
    onMonth: (YearMonth) -> Unit,
    onEdit: (Category) -> Unit,
) {
    val limits = data.limitsFor(month)
    val expenses = data.categories.filter { it.kind == CategoryKind.EXPENSE && (!it.archived || it.id in limits) }
        .sortedWith(compareByDescending<Category> { it.id in limits }.thenBy { it.name.lowercase() })
    SettingsColumn {
        MonthControl(month, availableMonths(data, current), onMonth)
        Text(
            "A limit applies from the selected month until you change it.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expenses.isEmpty()) {
            EmptyPanel(Icons.Outlined.Category, "No expense categories", "Create an expense category in Settings to set a limit.")
        } else {
            CategoryRows(expenses, onEdit) { category -> limits[category.id]?.let(money::format).orEmpty().ifBlank { "Set limit" } }
        }
    }
}

@Composable
internal fun LimitEditor(
    category: Category,
    month: YearMonth,
    effective: Long?,
    money: Money,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (MonthlyLimit) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(effective?.let(money::input).orEmpty()) }
    val amount = money.parse(value)?.takeIf { it > 0 }
    EditorFrame(
        title = if (effective == null) "Set monthly limit" else "Edit monthly limit",
        busy = busy, error = error, action = "Save limit", canSave = amount != null, onClose = onClose,
        onSave = { amount?.let { onSave(MonthlyLimit(category.id, month.toString(), it)) } },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category.glyph)
            Column {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "From " + monthLabel(month), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AmountField("Monthly limit", value, money, { value = it }, large = true)
        if (effective != null) {
            EditorOption("Remove limit from this month", Icons.Outlined.RemoveCircleOutline, !busy) {
                onSave(MonthlyLimit(category.id, month.toString(), 0))
            }
        }
    }
}
