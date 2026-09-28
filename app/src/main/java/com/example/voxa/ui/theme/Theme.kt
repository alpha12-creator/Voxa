package com.example.voxa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.voxa.data.preferences.AccentColor
import com.example.voxa.data.preferences.ThemeMode

data class VoxaColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentText: Color,
    val danger: Color,
    val isDark: Boolean
)

val LocalVoxaColors = staticCompositionLocalOf<VoxaColors> {
    error("No VoxaColors provided")
}

@Composable
fun VoxaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accent: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accentColor = accent.color
    val voxaColors = if (darkTheme) {
        VoxaColors(
            bg = VoxaDarkBg,
            surface = VoxaDarkSurface,
            surface2 = VoxaDarkSurface2,
            border = VoxaDarkBorder,
            text = VoxaDarkText,
            textSecondary = VoxaDarkTextSecondary,
            accent = accentColor,
            accentText = Color(0xFF0E1013),
            danger = VoxaDanger,
            isDark = true
        )
    } else {
        VoxaColors(
            bg = VoxaLightBg,
            surface = VoxaLightSurface,
            surface2 = VoxaLightSurface2,
            border = VoxaLightBorder,
            text = VoxaLightText,
            textSecondary = VoxaLightTextSecondary,
            accent = accentColor,
            accentText = Color.White,
            danger = VoxaDanger,
            isDark = false
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF0E1013),
            background = VoxaDarkBg,
            onBackground = VoxaDarkText,
            surface = VoxaDarkSurface,
            onSurface = VoxaDarkText,
            surfaceVariant = VoxaDarkSurface2,
            onSurfaceVariant = VoxaDarkTextSecondary,
            outline = VoxaDarkBorder,
            error = VoxaDanger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            background = VoxaLightBg,
            onBackground = VoxaLightText,
            surface = VoxaLightSurface,
            onSurface = VoxaLightText,
            surfaceVariant = VoxaLightSurface2,
            onSurfaceVariant = VoxaLightTextSecondary,
            outline = VoxaLightBorder,
            error = VoxaDanger,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = voxaColors.bg.toArgb()
                window.navigationBarColor = voxaColors.surface.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalVoxaColors provides voxaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
