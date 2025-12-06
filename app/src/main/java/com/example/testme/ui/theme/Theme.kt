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
    primary = BrandGreen500,
    onPrimary = Color.White,
    primaryContainer = BrandGreen100,
    onPrimaryContainer = BrandGreen600,

    secondary = BrandCyan500,
    onSecondary = Color.White,
    secondaryContainer = BrandCyan100,
    onSecondaryContainer = BrandCyan600,

    tertiary = BrandLime500,
    onTertiary = Color.White,

    background = UIBackground,
    onBackground = UIForeground,

    surface = UIBackground,
    onSurface = UIForeground,

    surfaceVariant = UISecondary,
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
