package com.example.testme.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.testme.ui.navigation.Screen
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SoftBlobBackground()
            FrontFacingRotatingPanelsBackground()
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.8f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Test.me",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 52.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI 기반 맞춤형 시험 생성 플랫폼",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF004D2E).copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        Spacer(modifier = Modifier.height(20.dp))

                        GradientButton(
                            text = "🧾 시험",
                            onClick = { navController.navigate(Screen.Dashboard.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )

                        GradientButton(
                            text = "👤 내 프로필",
                            onClick = { navController.navigate(Screen.Profile.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )

                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Powered by GPT-5 & Gemini",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FrontFacingRotatingPanelsBackground() {
    val lineColor = Color(0xFF005F3E)
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 20500
                0f at 0
                45f at 2000
                45f at 2500
                90f at 4500
                90f at 5000
                135f at 7000
                135f at 7500
                180f at 9500
                180f at 10000
                225f at 12000
                225f at 12500
                270f at 14500
                270f at 15000
                315f at 17000
                315f at 17500
                360f at 20000
                360f at 20500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, -size.height /4f)
        val radius = size.minDimension*1.5f
        val panelWidth = (size.width/6)*5
        val panelHeight = (size.height/7)*4

        for (i in 0 until 8) {
            val angle = ((i * 45f) + rotation) % 360f
            val rad = Math.toRadians(angle.toDouble())

            val x = center.x + radius * cos(rad).toFloat()
            val y = center.y + radius * sin(rad).toFloat()

            val depth = -cos(rad+90).toFloat()
            val scaleX = 0.4f + 0.6f * depth

            val paperColor = Color(0xFFFAFAF5)
            val borderColor = Color.Black.copy(alpha = 0.1f)
            val shadowColor = Color.Black.copy(alpha = 0.08f)

            val rectWidth = panelWidth * scaleX
            val rectHeight = panelHeight

            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(
                    x - rectWidth / 2f + 10f,
                    y - rectHeight / 2f + 10f
                ),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(20f, 20f)
            )

            drawRoundRect(
                color = paperColor,
                topLeft = Offset(x - rectWidth / 2f, y - rectHeight / 2f),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(20f, 20f)
            )

            drawRoundRect(
                color = borderColor,
                topLeft = Offset(x - rectWidth / 2f, y - rectHeight / 2f),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 3f)
            )

            val lineCount = 15
            val titleMarginTop = 100f
            val lineSpacing = (rectHeight-titleMarginTop) / (lineCount + 1)
            repeat(lineCount) { j ->
                val yOffset = y - rectHeight / 2f + titleMarginTop + (j + 1) * lineSpacing
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(x - rectWidth / 2f + 40f, yOffset),
                    end = Offset(x + rectWidth / 2f - 40f, yOffset),
                    strokeWidth = 4f
                )
            }

            drawLine(
                color = lineColor.copy(alpha = 0.7f),
                start = Offset(x - rectWidth / 2f + 20f, y - rectHeight / 2f + 100f),
                end = Offset(x + rectWidth / 2f - 20f, y - rectHeight / 2f + 100f),
                strokeWidth = 8f
            )
        }
    }
}

@Composable
fun SoftBlobBackground() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6FFFA))
    ) {
        // Blob 1 (Green)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFB3F6A5).copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.35f),
                radius = size.minDimension * 0.6f
            ),
            center = Offset(size.width * 0.3f, size.height * 0.35f),
            radius = size.minDimension * 0.6f
        )

        // Blob 2 (Mint Blue)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA5F6E8).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.8f, size.height * 0.25f),
                radius = size.minDimension * 0.5f
            ),
            center = Offset(size.width * 0.8f, size.height * 0.25f),
            radius = size.minDimension * 0.5f
        )

        // Blob 3 (Light Teal)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFAEEBFF).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.15f, size.height * 0.75f),
                radius = size.minDimension * 0.55f
            ),
            center = Offset(size.width * 0.15f, size.height * 0.75f),
            radius = size.minDimension * 0.55f
        )

        // Center light glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.minDimension * 0.7f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.minDimension * 0.7f
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00A86B), // 초록
            Color(0xFF0099CC)  // 청록
        )
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, shape = MaterialTheme.shapes.extraLarge)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            )
        }
    }
}
