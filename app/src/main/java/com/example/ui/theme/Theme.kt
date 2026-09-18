package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyanCore,
    onPrimary = JarvisVoidBlack,
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = JarvisCyanCore,
    secondary = JarvisBlueGlow,
    onSecondary = JarvisTextPrimary,
    secondaryContainer = JarvisSurfaceNavy,
    onSecondaryContainer = JarvisTextPrimary,
    tertiary = JarvisGoldAccent,
    onTertiary = JarvisVoidBlack,
    background = JarvisVoidBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisDeepNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceNavy,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorderGlow,
    outlineVariant = JarvisSurfaceElevated
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = JarvisColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = JarvisVoidBlack.toArgb()
                window.navigationBarColor = JarvisVoidBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
