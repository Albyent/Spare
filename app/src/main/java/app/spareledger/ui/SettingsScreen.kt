package app.spareledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.spareledger.ThemeMode
import java.util.Currency

@Composable
internal fun SettingsScreen(
    theme: ThemeMode,
    themeColor: ThemeColor,
    currency: String?,
    busy: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onThemeColor: (ThemeColor) -> Unit,
    onCategories: () -> Unit,
    onLimits: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onCsv: () -> Unit,
) {
    SettingsColumn {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("Appearance")
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeChoice(mode, themeColor, mode == theme, Modifier.weight(1f), onClick = { onTheme(mode) })
                }
            }
            if (theme == ThemeMode.SYSTEM) {
                Text(
                    "Matches your phone’s theme.", Modifier.padding(start = 3.dp, top = 5.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Theme color", Modifier.padding(start = 3.dp, top = 12.dp), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeColor.entries.forEach { choice ->
                    ColorChoice(choice, choice == themeColor, Modifier.weight(1f)) { onThemeColor(choice) }
                }
            }
        }
        if (currency != null) {
            Column {
                SectionTitle("Ledger")
                Panel {
                    Column {
                        SettingRow("Categories", Icons.Outlined.Category, onClick = onCategories)
                        Rule()
                        SettingRow("Monthly limits", Icons.Outlined.Tune, onClick = onLimits)
                        Rule()
                        Row(
                            Modifier.fillMaxWidth().padding(15.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Currency", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text(
                                currency + " · " + Currency.getInstance(currency).displayName,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Column {
            SectionTitle("Data")
            Panel {
                Column {
                    if (currency != null) {
                        SettingRow("Save backup", Icons.Outlined.SaveAlt, "Accounts, transactions, budgets, and limits", !busy, onBackup)
                        Rule()
                    }
                    SettingRow("Restore backup", Icons.Outlined.FileUpload, enabled = !busy, onClick = onRestore)
                    if (currency != null) {
                        Rule()
                        SettingRow("Export CSV", Icons.Outlined.FileDownload, "Transactions for a spreadsheet", !busy, onCsv)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsColumn(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = screenWidth).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = screenGutter, end = screenGutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
internal fun Rule() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ThemeChoice(
    mode: ThemeMode,
    color: ThemeColor,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
                when (mode) {
                    ThemeMode.SYSTEM -> {
                        MiniScreen(color, false, Modifier.weight(1f))
                        MiniScreen(color, true, Modifier.weight(1f))
                    }
                    ThemeMode.LIGHT -> MiniScreen(color, false, Modifier.weight(1f))
                    ThemeMode.DARK -> MiniScreen(color, true, Modifier.weight(1f))
                }
            }
            Text(mode.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ColorChoice(color: ThemeColor, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val sample = color.palette(false).hero
    Column(
        modifier.clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.size(36.dp).border(
                if (selected) 2.dp else 1.dp,
                if (selected) colors.primary else colors.outlineVariant,
                CircleShape,
            ).padding(4.dp).background(sample.start, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = sample.text)
        }
        Text(color.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MiniScreen(color: ThemeColor, dark: Boolean, modifier: Modifier) {
    val colors = remember(color, dark) { color.scheme(dark) }
    Column(
        modifier.height(54.dp).background(colors.background).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.5f).height(3.dp).background(colors.outline, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth().height(16.dp).background(color.palette(dark).hero.start, RoundedCornerShape(4.dp)))
        Box(Modifier.fillMaxWidth(0.72f).height(5.dp).background(colors.primaryContainer, RoundedCornerShape(2.dp)))
    }
}
