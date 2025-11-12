package com.example.testme.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.testme.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF5BA27F),
    onPrimary = Color.White,
    secondary = Color(0xFF3C7A5B),
    secondaryContainer = Color(0xFFE6F3EB),
    background = Color(0xFFF5F7F6),
    surface = Color.White,
    onSurface = Color(0xFF333333)
)

private val Pretendard = FontFamily(
    Font(R.font.pretendard_regular),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    headlineSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontSize = 16.sp)
)

@Composable
fun TestMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
