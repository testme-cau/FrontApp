package com.example.testme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val examHistory = listOf(
        ExamHistoryItem("운동생리학 챕터1", "2025-11-10", 9, 10, "GPT"),
        ExamHistoryItem("기능해부학 상지 근육", "2025-11-08", 7, 10, "Gemini"),
        ExamHistoryItem("병태생리학 순환기계", "2025-11-03", 10, 10, "GPT")
    )

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        containerColor = colors.secondaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "이전 시험 기록",
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
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (examHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "아직 푼 시험이 없습니다.",
                        color = colors.outline,
                        style = typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(examHistory) { exam ->
                        HistoryCard(
                            item = exam,
                            onClick = {
                                navController.navigate("result")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: ExamHistoryItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                item.title,
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "날짜: ${item.date}",
                    style = typography.bodyMedium.copy(color = colors.onSurface)
                )
                Text(
                    "AI: ${item.aiProvider}",
                    style = typography.bodyMedium.copy(color = colors.secondary)
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = item.score.toFloat() / item.total,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "점수: ${item.score} / ${item.total}",
                style = typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
            )
        }
    }
}

data class ExamHistoryItem(
    val title: String,
    val date: String,
    val score: Int,
    val total: Int,
    val aiProvider: String
)
