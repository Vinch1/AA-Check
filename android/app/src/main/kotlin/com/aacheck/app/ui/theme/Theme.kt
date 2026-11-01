package com.aacheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aacheck.app.core.ThemeMode
import com.aacheck.app.core.TokenStore

/**
 * Warm, editorial palette: ivory base, cognac/terracotta primary,
 * bronze-gold secondary — calm, simple, but quietly expensive.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF9C4A28),
    onPrimary = Color(0xFFFBEDE2),
    primaryContainer = Color(0xFFF4DDCD),
    onPrimaryContainer = Color(0xFF3A1606),
    inversePrimary = Color(0xFFE6B295),

    secondary = Color(0xFFB5895A),
    onSecondary = Color(0xFF2C1A08),
    secondaryContainer = Color(0xFFF2DEC4),
    onSecondaryContainer = Color(0xFF3A2810),

    tertiary = Color(0xFF7C6A4F),
    onTertiary = Color(0xFFFBF6EF),
    tertiaryContainer = Color(0xFFE7DBC4),
    onTertiaryContainer = Color(0xFF2E2516),

    background = Color(0xFFFBF5EE),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFDFAF5),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFF1E4D4),
    onSurfaceVariant = Color(0xFF6E5E4D),
    surfaceTint = Color(0xFF9C4A28),

    outline = Color(0xFFDCCBB8),
    outlineVariant = Color(0xFFEAD9C6),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFCEEEE),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6B295),
    onPrimary = Color(0xFF4A1E0C),
    primaryContainer = Color(0xFF7A3A20),
    onPrimaryContainer = Color(0xFFFBDDCC),
    inversePrimary = Color(0xFF9C4A28),

    secondary = Color(0xFFD9B98A),
    onSecondary = Color(0xFF3A260E),
    secondaryContainer = Color(0xFF553B20),
    onSecondaryContainer = Color(0xFFF4DEC1),

    tertiary = Color(0xFFCFC0A4),
    onTertiary = Color(0xFF332B1A),
    tertiaryContainer = Color(0xFF4A4030),
    onTertiaryContainer = Color(0xFFEBE0C9),

    background = Color(0xFF1B1510),
    onBackground = Color(0xFFEDE2D6),
    surface = Color(0xFF221B15),
    onSurface = Color(0xFFEDE2D6),
    surfaceVariant = Color(0xFF3B3025),
    onSurfaceVariant = Color(0xFFCBB9A4),
    surfaceTint = Color(0xFFE6B295),

    outline = Color(0xFF54463A),
    outlineVariant = Color(0xFF3F3328),

    error = Color(0xFFF2B8B0),
    onError = Color(0xFF410E0B),
    errorContainer = Color(0xFF410E0B),
    onErrorContainer = Color(0xFFF9DEDC),

    scrim = Color(0xFF000000),
)

/**
 * Serif for display/headline/title gives an editorial, boutique feel;
 * a clean sans carries the body for legibility.
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Holds the active theme mode and persists changes so any screen can toggle
 * the app theme and have the whole tree re-compose.
 */
class ThemeController(
    initial: ThemeMode,
    private val store: TokenStore,
) {
    var mode by mutableStateOf(initial)
        private set

    fun select(mode: ThemeMode) {
        this.mode = mode
        store.themeMode = mode
    }
}

val LocalThemeController = compositionLocalOf<ThemeController> {
    error("ThemeController not provided")
}

@Composable
fun AaCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

@Composable
fun rememberThemeController(store: TokenStore): ThemeController {
    return remember(store) { ThemeController(store.themeMode, store) }
}

@Composable
fun AaCheckAppTheme(
    controller: ThemeController,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (controller.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalThemeController provides controller) {
        AaCheckTheme(darkTheme = darkTheme, content = content)
    }
}
