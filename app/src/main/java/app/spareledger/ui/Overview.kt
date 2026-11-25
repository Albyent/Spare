package app.spareledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.spareledger.data.Category
import app.spareledger.data.LedgerEntry
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.Money
import app.spareledger.data.Spending
import app.spareledger.data.parseMonth
import app.spareledger.data.spending
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
internal fun Overview(
    data: LedgerSnapshot,
    today: LocalDate,
    month: YearMonth,
    money: Money,
    cardAmountsHidden: Boolean,
    onMonth: (YearMonth) -> Unit,
    onCategory: (Long) -> Unit,
    onActivity: () -> Unit,
    onEntry: (LedgerEntry) -> Unit,
    onLimits: () -> Unit,
    onBudget: () -> Unit,
    onToggleCardAmounts: () -> Unit,
) {
    val report = remember(data, month, today) { data.spending(month, today) }
    val currentMonth = YearMonth.from(today)
    val choices = remember(data, currentMonth) { availableMonths(data, currentMonth) }
    val categories = remember(data, report) {
        (report.categories.keys + report.limits.keys).mapNotNull(data.categoriesById::get)
            .filter { !it.archived || it.id in report.categories }
            .sortedWith(compareByDescending<Category> { report.categories[it.id] ?: 0 }.thenBy(Category::name))
    }
    val recent = remember(data, month, today) {
        val period = month.atDay(1).toEpochDay()..minOf(today, month.atEndOfMonth()).toEpochDay()
        data.entries.asSequence().filter { it.day in period }.take(4).toList()
    }
    val barTotal = report.categories.values.filter { it > 0 }.sum()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = screenWidth).fillMaxSize(),
            contentPadding = PaddingValues(start = screenGutter, end = screenGutter, top = 3.dp, bottom = bottomSpace),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthControl(month, choices, onMonth)
            }
            item {
                SpendingCard(report, money, month == currentMonth, cardAmountsHidden, onBudget, onToggleCardAmounts)
            }
            item {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("Spending by category", "Limits", onLimits)
                    if (categories.isEmpty()) {
                        EmptyPanel(
                            Icons.AutoMirrored.Outlined.ReceiptLong, "No spending recorded",
                            if (month == currentMonth) "Tap Add to log an expense." else null,
                        )
                    } else {
                        Panel {
                            Column {
                                categories.forEachIndexed { index, category ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    CategorySpending(
                                        category,
                                        report.categories[category.id] ?: 0,
                                        report.limits[category.id],
                                        barTotal,
                                        money,
                                        onClick = { onCategory(category.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle("Recent activity", "View all", onActivity)
                        Panel {
                            Column {
                                recent.forEachIndexed { index, entry ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    TransactionRow(entry, data, money, showDate = true, onClick = { onEntry(entry) })
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
private fun SpendingCard(
    report: Spending,
    money: Money,
    current: Boolean,
    hidden: Boolean,
    onBudget: () -> Unit,
    onToggle: () -> Unit,
) {
    val hero = LocalHero.current
    val remaining = report.remaining
    val overBudget = remaining != null && remaining < 0 && !hidden
    val heading = when {
        overBudget -> "Over budget"
        current -> "Left this month"
        else -> "Budget remaining"
    }
    val mainValue = when {
        remaining == null -> "Set budget"
        hidden -> hiddenAmount
        else -> money.format(abs(remaining))
    }
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(hero.start, hero.end))),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f).clip(MaterialTheme.shapes.medium).clickable(
                    onClickLabel = if (remaining == null) "Set monthly budget" else "Edit monthly budget",
                    role = Role.Button,
                    onClick = onBudget,
                ).padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 15.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(heading, color = hero.muted, style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Outlined.Edit, null, Modifier.size(14.dp), tint = hero.muted)
                }
                AmountText(
                    mainValue, Modifier.fillMaxWidth().padding(top = 3.dp).amountVisibility(mainValue),
                    style = MaterialTheme.typography.displayLarge, color = if (overBudget) hero.negative else hero.text,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (hidden) "Show card amounts" else "Hide card amounts",
                    modifier = Modifier.size(21.dp),
                    tint = hero.text,
                )
            }
        }
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 20.dp)) {
            HorizontalDivider(color = hero.divider)
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val budget = report.budget?.let { if (hidden) hiddenAmount else money.format(it) } ?: "—"
                val spent = if (hidden) hiddenAmount else money.format(report.spent)
                SpendingMetric("Monthly budget", budget, Modifier.weight(1f))
                SpendingMetric(if (current) "Spent this month" else "Total spent", spent, Modifier.weight(1f))
            }
        }
    }
}

private const val hiddenAmount = "••••"

private fun Modifier.amountVisibility(value: String): Modifier =
    if (value == hiddenAmount) clearAndSetSemantics { contentDescription = "Amount hidden" } else this

@Composable
private fun SpendingMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalHero.current.muted)
        AmountText(
            value, Modifier.fillMaxWidth().amountVisibility(value),
            color = LocalHero.current.text, style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun CategorySpending(
    category: Category,
    spent: Long,
    limit: Long?,
    total: Long,
    money: Money,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val progress = if (total == 0L) 0f else (spent.toDouble() / total).toFloat().coerceIn(0f, 1f)
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp).heightIn(min = 42.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            CategoryBadge(category.glyph)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(category.name, Modifier.weight(1.15f), style = MaterialTheme.typography.titleSmall)
                    AmountText(
                        money.format(spent), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, alignment = TextAlign.End,
                    )
                }
                if (progress > 0f || limit != null) {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.surfaceVariant)) {
                        Box(
                            Modifier.fillMaxWidth(progress).height(4.dp)
                                .background(if (limit != null && spent > limit) colors.error else colors.primary),
                        )
                    }
                }
                if (limit != null) {
                    val left = limit - spent
                    Text(
                        if (left >= 0) money.format(left) + " left" else money.format(-left) + " over limit",
                        color = if (left < 0) colors.error else colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MonthControl(month: YearMonth, choices: List<YearMonth>, onMonth: (YearMonth) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val newest = choices.first()
    val oldest = choices.last()
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMonth(month.minusMonths(1)) }, enabled = month > oldest) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month")
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TextButton(onClick = { expanded = true }) {
                Text(monthLabel(month), style = MaterialTheme.typography.titleSmall)
                Icon(Icons.Outlined.ExpandMore, contentDescription = "Choose month", modifier = Modifier.padding(start = 3.dp))
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(monthLabel(choice)) },
                        onClick = { expanded = false; onMonth(choice) },
                    )
                }
            }
        }
        IconButton(onClick = { onMonth(month.plusMonths(1)) }, enabled = month < newest) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month")
        }
    }
}

internal fun availableMonths(data: LedgerSnapshot, current: YearMonth): List<YearMonth> =
    ((0L..11L).map(current::minusMonths) +
        data.entries.map { YearMonth.from(LocalDate.ofEpochDay(it.day)) } +
        data.limits.mapNotNull { parseMonth(it.month) } +
        data.budgets.mapNotNull { parseMonth(it.month) }).filter { it <= current }.distinct().sortedDescending()

internal fun monthLabel(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("LLLL yyyy"))
