package ru.tabel.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Primary   = Color(0xFF4F6EF7)
val Secondary = Color(0xFF8B5CF6)
val Orange    = Color(0xFFf97316)
val Green     = Color(0xFF22c55e)

private val DarkColors = darkColorScheme(
    primary              = Color(0xFF4F6EF7),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF1A2A6C),
    onPrimaryContainer   = Color(0xFFDDE1FF),
    secondary            = Color(0xFF8B5CF6),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFF2D1B6E),
    onSecondaryContainer = Color(0xFFEDE9FE),
    background           = Color(0xFF0E0E0E),
    onBackground         = Color(0xFFEEEEEE),
    surface              = Color(0xFF1A1A1A),
    onSurface            = Color(0xFFEEEEEE),
    surfaceVariant       = Color(0xFF252525),
    onSurfaceVariant     = Color(0xFF9A9A9A),
    outline              = Color(0xFF333333),
    error                = Color(0xFFFF6B6B),
    tertiary             = Orange,
    onTertiary           = Color(0xFFFFFFFF)
)

private val LightColors = lightColorScheme(
    primary              = Color(0xFF4F6EF7),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFDDE1FF),
    onPrimaryContainer   = Color(0xFF001684),
    secondary            = Color(0xFF8B5CF6),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF261E6C),
    background           = Color(0xFFF4F5FF),
    onBackground         = Color(0xFF1A1C2E),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1C2E),
    surfaceVariant       = Color(0xFFEEEFF8),
    onSurfaceVariant     = Color(0xFF44475A),
    outline              = Color(0xFFCCCEE0),
    error                = Color(0xFFEF4444)
)

@Composable
fun TabelTheme(
    darkTheme: Boolean    = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
