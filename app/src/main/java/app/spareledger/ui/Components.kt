package app.spareledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spareledger.data.AccountKind
import app.spareledger.data.Glyph

internal val screenWidth = 600.dp
internal val screenGutter = 20.dp
internal val bottomSpace = 22.dp

@Composable
internal fun SpareHeader(title: String, onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxWidth().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.widthIn(max = screenWidth).fillMaxWidth().heightIn(min = 62.dp)
                    .padding(start = if (onBack == null) screenGutter else 6.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    title, Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onSettings != null) {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
internal fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Badge(icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, modifier)
}

@Composable
internal fun CategoryBadge(glyph: Glyph, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (background, ink) = when (glyph) {
        Glyph.DINING, Glyph.SHOPPING, Glyph.LEISURE -> colors.tertiaryContainer to colors.onTertiaryContainer
        Glyph.TRANSPORT, Glyph.BILLS, Glyph.TRAVEL -> colors.secondaryContainer to colors.onSecondaryContainer
        Glyph.HEALTH -> colors.errorContainer to colors.onErrorContainer
        else -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Badge(glyph.icon, background, ink, modifier)
}

@Composable
private fun Badge(icon: ImageVector, background: Color, ink: Color, modifier: Modifier) {
    Box(
        modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = ink)
    }
}

internal val Glyph.icon: ImageVector
    get() = when (this) {
        Glyph.GROCERIES -> Icons.Outlined.LocalGroceryStore
        Glyph.DINING -> Icons.Outlined.Restaurant
        Glyph.TRANSPORT -> Icons.Outlined.DirectionsBus
        Glyph.HOME -> Icons.Outlined.Home
        Glyph.BILLS -> Icons.AutoMirrored.Outlined.ReceiptLong
        Glyph.SHOPPING -> Icons.Outlined.ShoppingBag
        Glyph.HEALTH -> Icons.Outlined.FavoriteBorder
        Glyph.LEISURE -> Icons.Outlined.SportsEsports
        Glyph.TRAVEL -> Icons.Outlined.Flight
        Glyph.INCOME -> Icons.Outlined.Payments
        Glyph.OTHER -> Icons.Outlined.Category
    }

internal val AccountKind.icon: ImageVector
    get() = when (this) {
        AccountKind.CASH -> Icons.Outlined.Payments
        AccountKind.BANK -> Icons.Outlined.AccountBalance
        AccountKind.WALLET -> Icons.Outlined.AccountBalanceWallet
        AccountKind.CARD -> Icons.Outlined.CreditCard
    }

@Composable
internal fun PickField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@Composable
internal fun SettingRow(
    title: String,
    icon: ImageVector,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick, enabled = enabled, color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().heightIn(min = 65.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun EmptyPanel(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Panel {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconBadge(icon)
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (detail != null) {
                Text(
                    detail, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
internal fun AmountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    alignment: TextAlign = TextAlign.Start,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val nominal = style.copy(fontFeatureSettings = "tnum")
        val width = measurer.measure(text, nominal, softWrap = false, maxLines = 1).size.width
        val ratio = if (width > constraints.maxWidth) constraints.maxWidth.toFloat() / width else 1f
        val fitted = nominal.copy(fontSize = (style.fontSize.value * ratio).coerceAtLeast(11f).sp)
        Text(text, Modifier.fillMaxWidth(), color = color, style = fitted, textAlign = alignment)
    }
}
