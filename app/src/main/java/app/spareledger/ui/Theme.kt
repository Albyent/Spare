package app.spareledger.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val spareLight = lightColorScheme(
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

private val spareDark = darkColorScheme(
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
    val muted = lerp(text, end, 0.20f)
    val divider = lerp(start, text, 0.30f)
    val negative = Color(0xFFFFC9B6)
}

internal data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    val hero: HeroColors,
)

private fun palette(primary: Long, ink: Long, container: Long, onContainer: Long, start: Long, end: Long) =
    ThemePalette(Color(primary), Color(ink), Color(container), Color(onContainer), HeroColors(Color(start), Color(end)))

internal enum class ThemeColor(val label: String, private val light: ThemePalette, private val dark: ThemePalette) {
    FOREST(
        "Forest",
        palette(0xFF315E48, 0xFFFFFDF6, 0xFFDDEADC, 0xFF20402E, 0xFF294F3B, 0xFF19382F),
        palette(0xFFB5D7BD, 0xFF193324, 0xFF2D4737, 0xFFD8ECDD, 0xFF28463A, 0xFF20392F),
    ),
    OCEAN(
        "Ocean",
        palette(0xFF2B566A, 0xFFFFFDF6, 0xFFDCEBF0, 0xFF173D51, 0xFF31596B, 0xFF203E50),
        palette(0xFFA7D4E6, 0xFF172F3E, 0xFF293F4C, 0xFFD6EEF5, 0xFF2C4858, 0xFF243C49),
    ),
    PLUM(
        "Plum",
        palette(0xFF684B70, 0xFFFFFDF6, 0xFFEDE2EF, 0xFF472B52, 0xFF594361, 0xFF3D2B49),
        palette(0xFFDEC0E6, 0xFF3B2B42, 0xFF45344A, 0xFFF0DEF4, 0xFF473B50, 0xFF352B40),
    ),
    CLAY(
        "Clay",
        palette(0xFF834E3C, 0xFFFFFDF6, 0xFFF5E2D8, 0xFF5D3328, 0xFF694737, 0xFF513429),
        palette(0xFFF2C1AB, 0xFF482C23, 0xFF4D362D, 0xFFF7DCD0, 0xFF533C32, 0xFF422F28),
    ),
    GOLD(
        "Gold",
        palette(0xFF755823, 0xFFFFFDF6, 0xFFF3E7CD, 0xFF513A17, 0xFF5B4928, 0xFF41351E),
        palette(0xFFEBD39C, 0xFF3D3018, 0xFF4B3F26, 0xFFF4E5C6, 0xFF514329, 0xFF3D3423),
    ),
    ;

    fun palette(isDark: Boolean): ThemePalette = if (isDark) dark else light

    fun scheme(isDark: Boolean): ColorScheme {
        val base = if (isDark) spareDark else spareLight
        val active = palette(isDark)
        return base.copy(
            primary = active.primary,
            onPrimary = active.onPrimary,
            primaryContainer = active.container,
            onPrimaryContainer = active.onContainer,
            inversePrimary = palette(!isDark).primary,
            secondary = active.primary,
            onSecondary = active.onPrimary,
            secondaryContainer = lerp(base.secondaryContainer, active.container, 0.72f),
            onSecondaryContainer = active.onContainer,
            surfaceTint = active.primary,
        )
    }
}

internal val LocalHero = staticCompositionLocalOf { ThemeColor.FOREST.palette(false).hero }

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
internal fun SpareTheme(dark: Boolean, color: ThemeColor, content: @Composable () -> Unit) {
    val scheme = remember(dark, color) { color.scheme(dark) }
    CompositionLocalProvider(LocalHero provides color.palette(dark).hero) {
        MaterialTheme(
            colorScheme = scheme,
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
