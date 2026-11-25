package app.spareledger.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SpareLight = lightColorScheme(
    primary = Color(0xFF315E48),
    onPrimary = Color(0xFFFFFDF6),
    primaryContainer = Color(0xFFDDEADC),
    onPrimaryContainer = Color(0xFF20402E),
    secondary = Color(0xFF4A625E),
    secondaryContainer = Color(0xFFE1ECE8),
    onSecondaryContainer = Color(0xFF254842),
    tertiary = Color(0xFF825330),
    tertiaryContainer = Color(0xFFF3E7D8),
    onTertiaryContainer = Color(0xFF663D22),
    background = Color(0xFFF6F3EB),
    onBackground = Color(0xFF202B23),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF202B23),
    surfaceVariant = Color(0xFFEFEEE5),
    onSurfaceVariant = Color(0xFF536157),
    surfaceContainer = Color(0xFFF0EEE5),
    surfaceContainerHigh = Color(0xFFEAE9DF),
    outline = Color(0xFF738075),
    outlineVariant = Color(0xFFD7DBCF),
    error = Color(0xFFA43A38),
    errorContainer = Color(0xFFF9E4DF),
    onErrorContainer = Color(0xFF822B2B),
)

internal val SpareDark = darkColorScheme(
    primary = Color(0xFFB5D7BD),
    onPrimary = Color(0xFF193324),
    primaryContainer = Color(0xFF2D4737),
    onPrimaryContainer = Color(0xFFD8ECDD),
    secondary = Color(0xFFAFCDC5),
    secondaryContainer = Color(0xFF2B413C),
    onSecondaryContainer = Color(0xFFD3E8E2),
    tertiary = Color(0xFFE9BE98),
    tertiaryContainer = Color(0xFF47382C),
    onTertiaryContainer = Color(0xFFF4D7BB),
    background = Color(0xFF171C18),
    onBackground = Color(0xFFF1EEE3),
    surface = Color(0xFF222921),
    onSurface = Color(0xFFF1EEE3),
    surfaceVariant = Color(0xFF2C342C),
    onSurfaceVariant = Color(0xFFB9C4B9),
    surfaceContainer = Color(0xFF262E25),
    surfaceContainerHigh = Color(0xFF303930),
    outline = Color(0xFF98A498),
    outlineVariant = Color(0xFF404C41),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF542F30),
    onErrorContainer = Color(0xFFFFDAD4),
)

internal data class HeroColors(val start: Color, val end: Color) {
    val text = Color(0xFFFFF8E9)
    val muted = Color(0xFFCFDED1)
    val divider = Color(0xFF648274)
    val positive = Color(0xFFC6E7CB)
    val negative = Color(0xFFFFC9B6)
}

private val lightHero = HeroColors(Color(0xFF294F3B), Color(0xFF19382F))
private val darkHero = HeroColors(Color(0xFF28463A), Color(0xFF20392F))
internal val LocalHero = staticCompositionLocalOf { lightHero }

private val plain = FontFamily.SansSerif
private val display = FontFamily.Serif

private fun type(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = plain,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

private val spareTypography = Typography(
    displayLarge = type(47, 55, family = display).copy(letterSpacing = (-1.5).sp),
    headlineLarge = type(33, 39, family = display).copy(letterSpacing = (-0.7).sp),
    headlineMedium = type(27, 34, family = display).copy(letterSpacing = (-0.4).sp),
    headlineSmall = type(22, 29, FontWeight.SemiBold),
    titleLarge = type(19, 26, FontWeight.SemiBold),
    titleMedium = type(16, 22, FontWeight.Medium),
    titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 21),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 17, FontWeight.Medium),
    labelSmall = type(11, 15, FontWeight.Medium),
)

@Composable
internal fun SpareTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHero provides if (dark) darkHero else lightHero) {
        MaterialTheme(
            colorScheme = if (dark) SpareDark else SpareLight,
            typography = spareTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}
