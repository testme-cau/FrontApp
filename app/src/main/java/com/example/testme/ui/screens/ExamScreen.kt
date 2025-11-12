package com.example.testme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun ExamScreen(navController: NavController) {
    var currentIndex by remember { mutableStateOf(0) }

    val questions = listOf(
        "운동 생리학에서 ATP-PCr 시스템은 몇 초 동안 에너지를 공급하나요?",
        "심박수의 최대치는 대략적으로 어떤 공식으로 계산할 수 있나요?",
        "젖산 역치(Lactate Threshold)는 무엇을 의미하나요?"
    )

    var answers by remember { mutableStateOf(List(questions.size) { "" }) }

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = colors.secondaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "시험 ${currentIndex + 1} / ${questions.size}",
                        style = typography.titleLarge.copy(
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "문제 ${currentIndex + 1}",
                        style = typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = questions[currentIndex],
                        style = typography.bodyMedium.copy(
                            color = colors.onSurface
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = answers[currentIndex],
                        onValueChange = { new ->
                            answers = answers.toMutableList().also { it[currentIndex] = new }
                        },
                        label = { Text("답을 입력하세요", color = colors.secondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = { currentIndex-- },
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.secondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(colors.secondary)
                        )
                    ) {
                        Text("이전", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(Modifier.width(80.dp))
                }

                if (currentIndex < questions.lastIndex) {
                    Button(
                        onClick = { currentIndex++ },
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text("다음", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { navController.navigate("result") },
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text("제출하기", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
