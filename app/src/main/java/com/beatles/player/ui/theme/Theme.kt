package com.beatles.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.beatles.player.data.ThemeMode

val LightBackground = Color(0xFFF6F7F9)
val LightSurface = Color(0xFFFFFFFF)
val TextLightPrimary = Color(0xFF141416)
val TextLightSecondary = Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkBackground,
    primary = Accent,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    primary = Color(0xFF141416),
    onBackground = TextLightPrimary,
    onSurface = TextLightPrimary
)

/** Snapshot-backed palette: reading it anywhere (composition or draw) reacts to theme changes. */
object AppTheme {
    var isDark by mutableStateOf(true)
    /** Foreground (text / icon / subtle fill) colour, white in dark mode and near-black in light mode. */
    fun fg(alpha: Float = 1f): Color = (if (isDark) Color.White else Color(0xFF141416)).copy(alpha = alpha)
    val bg: Color get() = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
    val card: Color get() = if (isDark) Color(0xFFE2E2E2).copy(alpha = 0.12f) else Color.White
    val sheet: Color get() = if (isDark) Color(0xFF1E2024) else Color(0xFFFFFFFF)
    /** Frosted translucent surface tint used by nav bar / popovers. */
    val glass: Color get() = if (isDark) Color(0xFF2A2A34).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.72f)
}

@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
}

@Composable
fun BeatlesTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = isDarkTheme(themeMode)
    AppTheme.isDark = isDark

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = view.context as? Activity
                    ?: (view.context as? android.content.ContextWrapper)?.baseContext as? Activity
                if (activity != null) {
                    val window = activity.window
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    val bgColor = if (isDark) DarkBackground.toArgb() else LightBackground.toArgb()
                    window.statusBarColor = bgColor
                    window.navigationBarColor = bgColor
                    insetsController.isAppearanceLightStatusBars = !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                }
            } catch (_: Throwable) {}
        }
    }
    MaterialTheme(colorScheme = if (isDark) DarkColorScheme else LightColorScheme, content = content)
}
