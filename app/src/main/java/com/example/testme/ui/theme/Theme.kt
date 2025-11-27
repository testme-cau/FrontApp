package com.example.testme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepGreen,
    secondary = MintGreen,
    tertiary = AquaBlue,
    background = SoftBackground,
    surface = OffWhite,
    onPrimary = Color.White,
    onBackground = DarkTeal,
)

@Composable
fun TestMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
