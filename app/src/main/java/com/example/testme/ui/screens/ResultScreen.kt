package com.example.testme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController) {
    val score = 8
    val total = 10
    val results = listOf(
        ResultItem("ATP-PCr 시스템은 몇 초?", "10초", "정답: 약 10초, 고강도 운동 초반 사용"),
        ResultItem("심박수 계산 공식?", "220 - 나이", "정답: 220 - 나이, 개인차 있음"),
        ResultItem("젖산 역치란?", "지속 가능한 최대 강도", "정답: 젖산이 급격히 증가하기 시작하는 지점")
    )

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        containerColor = colors.secondaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "시험 결과",
                        style = typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.secondary
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .height(160.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "점수",
                            style = typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = colors.secondary
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$score / $total",
                            style = typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = score.toFloat() / total,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(10.dp),
                            color = colors.primary,
                            trackColor = colors.primary.copy(alpha = 0.2f)
                        )
                    }
                }

            }

            Text(
                "문항별 결과",
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.secondary
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(results) { index, item ->
                    ResultCard(index + 1, item)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { navController.navigate("history") },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.secondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(colors.secondary)
                    )
                ) {
                    Text("이전 시험 보기", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { navController.navigate("home") },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("홈으로", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ResultCard(number: Int, result: ResultItem) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "문제 $number",
                style = typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            )
            Text(
                "내 답변: ${result.userAnswer}",
                style = typography.bodyMedium.copy(color = colors.onSurface)
            )
            Text(
                result.explanation,
                style = typography.bodyMedium.copy(color = colors.secondary)
            )
        }
    }
}

// 데이터 모델
data class ResultItem(
    val question: String,
    val userAnswer: String,
    val explanation: String
)
