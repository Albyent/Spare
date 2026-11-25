package app.spareledger.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.spareledger.data.EntryKind
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.Money
import app.spareledger.data.parseMonth
import app.spareledger.data.sourceDelta
import java.io.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal data class ActivityFilter(
    val query: String = "",
    val month: String? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
) : Serializable

@Composable
internal fun ActivityScreen(
    data: LedgerSnapshot,
    money: Money,
    today: LocalDate,
    filter: ActivityFilter,
    onFilter: (ActivityFilter) -> Unit,
    onEntry: (LedgerEntry) -> Unit,
) {
    val months = remember(data, today) { availableMonths(data, YearMonth.from(today)) }
    val result = remember(data, filter) { filteredEntries(data, filter).groupBy(LedgerEntry::day) }
    val listState = rememberLazyListState()
    LaunchedEffect(filter) { listState.scrollToItem(0) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = screenWidth).fillMaxSize(), state = listState,
            contentPadding = PaddingValues(start = screenGutter, end = screenGutter, bottom = bottomSpace),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item("filters") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { if (it.length <= 100) onFilter(filter.copy(query = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = if (filter.query.isNotEmpty()) {
                            {
                                IconButton(onClick = { onFilter(filter.copy(query = "")) }) {
                                    Icon(Icons.Outlined.Close, "Clear search")
                                }
                            }
                        } else null,
                        label = { Text("Search transactions") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            FilterMenu(
                                filter.month?.let(::parseMonth)?.let(::monthLabel) ?: "All dates",
                                filter.month != null,
                                listOf("All dates" to null) + months.map { monthLabel(it) to it.toString() },
                            ) { onFilter(filter.copy(month = it)) }
                            FilterMenu(
                                data.accountsById[filter.accountId]?.name ?: "All accounts",
                                filter.accountId != null,
                                listOf("All accounts" to null) + data.accounts.map { it.name to it.id },
                            ) { onFilter(filter.copy(accountId = it)) }
                            FilterMenu(
                                data.categoriesById[filter.categoryId]?.name ?: "All categories",
                                filter.categoryId != null,
                                listOf("All categories" to null) + data.categories.map { it.name to it.id },
                            ) { onFilter(filter.copy(categoryId = it)) }
                        }
                        if (filter.month != null || filter.accountId != null || filter.categoryId != null) {
                            IconButton(onClick = { onFilter(ActivityFilter(query = filter.query)) }) {
                                Icon(Icons.Outlined.FilterAltOff, "Clear filters")
                            }
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                item("empty") {
                    val filtering = filter != ActivityFilter()
                    EmptyPanel(
                        if (filtering) Icons.Outlined.Search else Icons.AutoMirrored.Outlined.ReceiptLong,
                        if (filtering) "No matching transactions" else "No transactions yet",
                        if (filtering) "Try another search or clear the filters." else "Tap Add to record your first transaction.",
                    )
                }
            }
            for ((day, entries) in result) {
                item("day-$day") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            dayLabel(LocalDate.ofEpochDay(day), today),
                            Modifier.padding(start = 4.dp, top = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Panel {
                            Column {
                                entries.forEachIndexed { index, entry ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    TransactionRow(entry, data, money, filter.accountId, onClick = { onEntry(entry) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterMenu(label: String, selected: Boolean, options: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected, onClick = { expanded = true },
            label = {
                Text(label, Modifier.widthIn(max = 134.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null, Modifier.size(17.dp)) },
        )
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for ((name, value) in options) {
                DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

private fun filteredEntries(data: LedgerSnapshot, filter: ActivityFilter): List<LedgerEntry> {
    val month = filter.month?.let(::parseMonth)
    val days = month?.let { it.atDay(1).toEpochDay()..it.atEndOfMonth().toEpochDay() }
    val query = filter.query.trim()
    return data.entries.filter { entry ->
        val accountMatches = filter.accountId == null || filter.accountId in listOf(entry.accountId, entry.destinationId)
        val categoryMatches = filter.categoryId == null || filter.categoryId == entry.categoryId
        val dayMatches = days == null || entry.day in days
        accountMatches && categoryMatches && dayMatches && (
            query.isEmpty() || listOf(
                entry.payee, entry.note, entry.kind.label, data.accountsById[entry.accountId]?.name.orEmpty(),
                data.accountsById[entry.destinationId]?.name.orEmpty(), data.categoriesById[entry.categoryId]?.name.orEmpty(),
            ).any { it.contains(query, ignoreCase = true) }
        )
    }
}

@Composable
internal fun TransactionRow(
    entry: LedgerEntry,
    data: LedgerSnapshot,
    money: Money,
    accountId: Long? = null,
    showDate: Boolean = false,
    onClick: () -> Unit,
) {
    val category = data.categoriesById[entry.categoryId]
    val source = data.accountsById.getValue(entry.accountId).name
    val destination = data.accountsById[entry.destinationId]?.name
    val transfer = entry.kind == EntryKind.TRANSFER
    val title = if (transfer) "$source → $destination" else entry.payee.ifBlank { category?.name ?: entry.kind.label }
    val subtitle = buildList {
        if (transfer || entry.kind == EntryKind.REFUND) add(entry.kind.label)
        if (!transfer) {
            if (category != null && title != category.name) add(category.name)
            add(source)
        }
        if (showDate) add(shortDate(LocalDate.ofEpochDay(entry.day)))
    }.joinToString(" · ")
    val delta = when {
        transfer && accountId == null -> entry.minor
        transfer && accountId == entry.destinationId -> entry.minor
        else -> entry.sourceDelta()
    }
    val positive = delta > 0 && (!transfer || accountId != null)
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = 65.dp).padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (category != null) CategoryBadge(category.glyph) else IconBadge(Icons.Outlined.SwapHoriz)
            Column(Modifier.weight(1.65f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            AmountText(
                money.format(delta, signed = positive), Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = if (positive) colors.primary else colors.onSurface, alignment = TextAlign.End,
            )
        }
    }
}

private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today, " + shortDate(day)
    today.minusDays(1) -> "Yesterday, " + shortDate(day)
    else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

internal fun shortDate(day: LocalDate): String = day.format(DateTimeFormatter.ofPattern("MMM d"))
