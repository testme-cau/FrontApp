package com.example.testme.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = UIPrimary,
    onPrimary = Color.White,
    secondary = UISecondary,
    onSecondary = UIForeground,
    tertiary = BrandGreen500, // Use Brand Green as tertiary for accents
    background = UIBackground,
    onBackground = UIForeground,
    surface = UIBackground,
    onSurface = UIForeground,
    surfaceVariant = UISecondary, // Slightly different surface
    onSurfaceVariant = UIForeground,
    outline = UIBorder,
    error = UIDestructive
)

@Composable
fun TestMeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
