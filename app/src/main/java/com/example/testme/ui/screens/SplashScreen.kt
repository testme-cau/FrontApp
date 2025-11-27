package com.example.testme.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(1800)
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    val infinite = rememberInfiniteTransition()

    val blob1X by infinite.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            tween(4500, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    val blob1Y by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    val blob2X by infinite.animateFloat(
        initialValue = 300f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            tween(5200, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    val blob3Y by infinite.animateFloat(
        initialValue = 500f,
        targetValue = 250f,
        animationSpec = infiniteRepeatable(
            tween(4800, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF5BA27F).copy(alpha = 0.40f), Color.Transparent),
                    radius = size.minDimension * 0.9f,
                    center = Offset(blob1X + size.width * 0.3f, blob1Y + size.height * 0.25f)
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF8FE2B2).copy(alpha = 0.35f), Color.Transparent),
                    radius = size.minDimension * 0.75f,
                    center = Offset(blob2X + size.width * 0.7f, size.height * 0.45f)
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFD6F8E6).copy(alpha = 0.50f), Color.Transparent),
                    radius = size.minDimension * 0.8f,
                    center = Offset(size.width * 0.45f, blob3Y)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Test.me",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 52.sp,
                    color = Color(0xFF1E4032)
                )
            )
            Text(
                "AI 시험 생성 플랫폼",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    color = Color(0xFF1E4032).copy(alpha = 0.85f)
                )
            )
        }
    }
}
