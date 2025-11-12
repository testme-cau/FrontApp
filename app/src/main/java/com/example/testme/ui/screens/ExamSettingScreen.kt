package com.example.testme.ui.screens

import androidx.compose.foundation.layout.*
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
fun ExamSettingsScreen(navController: NavController) {
    var questionCount by remember { mutableStateOf(10f) }
    var difficulty by remember { mutableStateOf("Medium") }

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        containerColor = colors.secondaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "시험 설정",
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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "문항 수 설정",
                        style = typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.secondary
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${questionCount.toInt()} 문항",
                        style = typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    )
                    Slider(
                        value = questionCount,
                        onValueChange = { questionCount = it },
                        valueRange = 5f..20f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "난이도 선택",
                        style = typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.secondary
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach {
                            FilterChip(
                                selected = difficulty == it,
                                onClick = { difficulty = it },
                                label = {
                                    Text(
                                        it,
                                        style = typography.bodyMedium.copy(
                                            fontWeight = if (difficulty == it) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.onPrimary,
                                    containerColor = colors.secondaryContainer,
                                    labelColor = colors.secondary
                                )
                            )
                        }
                    }
                }
            }

            // 🔹 버튼
            Button(
                onClick = { navController.navigate("exam") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text(
                    "시험 생성하기",
                    style = typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
