package com.example.testme.ui.screens.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.ExamDetailResponse
import com.example.testme.data.model.ExamResultResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Summarize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    examId: String,
    viewModel: ExamDetailViewModel = viewModel(
        factory = ExamDetailViewModelFactory(apiService, token, subjectId, examId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadDetail()
        viewModel.loadResult()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "시험 상세" }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadDetail()
                            viewModel.loadResult()
                        },
                        enabled = !uiState.loadingDetail && !uiState.loadingResult
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (uiState.loadingDetail && uiState.detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExamMetaCard(uiState)

                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "여기에서 실제 시험 응시 UI를 구현하면 됩니다."
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text("시험 풀기 (UI 구현 예정)")
                }

                ExamResultCard(uiState)

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (uiState.errorMessage != null) {
            LaunchedEffect(uiState.errorMessage) {
                snackbarHostState.showSnackbar(uiState.errorMessage ?: "오류가 발생했습니다.")
            }
        }
    }
}

@Composable
private fun ExamMetaCard(ui: ExamDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = ui.title.ifBlank { "시험 #${ui.examId.takeLast(6)}" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "시험 ID: ${ui.examId}",
                style = MaterialTheme.typography.bodySmall
            )
            if (ui.numQuestions != null) {
                Text(
                    text = "문항 수: ${ui.numQuestions}문항",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!ui.language.isNullOrBlank() || !ui.difficulty.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!ui.language.isNullOrBlank()) {
                        Text(
                            text = "언어: ${ui.language}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!ui.difficulty.isNullOrBlank()) {
                        Text(
                            text = "난이도: ${ui.difficulty}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (!ui.status.isNullOrBlank()) {
                Text(
                    text = "상태: ${ui.status}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!ui.createdAt.isNullOrBlank()) {
                Text(
                    text = "생성일: ${ui.createdAt}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ExamResultCard(ui: ExamDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "채점 결과",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (ui.loadingResult) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    OutlinedButton(
                        onClick = { ui.onRefreshResult?.invoke() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text("결과 새로고침")
                    }
                }
            }

            when {
                ui.loadingResult && ui.result == null -> {
                    Text(
                        text = "결과를 불러오는 중입니다...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ui.result == null -> {
                    Text(
                        text = "아직 제출된 결과가 없습니다.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                else -> {
                    if (ui.score != null && ui.totalScore != null) {
                        Text(
                            text = "점수: ${ui.score} / ${ui.totalScore}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (ui.correctCount != null && ui.totalCount != null) {
                        Text(
                            text = "정답 ${ui.correctCount}문항 / 총 ${ui.totalCount}문항",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!ui.summary.isNullOrBlank()) {
                        Text(
                            text = ui.summary ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

data class ExamDetailUiState(
    val loadingDetail: Boolean = false,
    val loadingResult: Boolean = false,
    val detail: ExamDetailResponse? = null,
    val result: ExamResultResponse? = null,
    val examId: String = "",
    val title: String = "",
    val numQuestions: Int? = null,
    val language: String? = null,
    val difficulty: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val score: Int? = null,
    val totalScore: Double? = null,
    val correctCount: Int? = null,
    val totalCount: Int? = null,
    val summary: String? = null,
    val errorMessage: String? = null,
    val onRefreshResult: (() -> Unit)? = null
)

class ExamDetailViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val examId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExamDetailUiState(
            examId = examId,
            onRefreshResult = { loadResult() }
        )
    )
    val uiState: StateFlow<ExamDetailUiState> = _uiState

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingDetail = true, errorMessage = null)
            try {
                val response: ExamDetailResponse =
                    apiService.getExamDetail("Bearer $token", subjectId, examId)

                // 실제 ExamDetailResponse 구조에 맞게 아래 필드를 매핑해서 사용하면 된다
                val exam = response.exam

                _uiState.value = _uiState.value.copy(
                    loadingDetail = false,
                    detail = response,
                    title = exam.title ?: "",
                    numQuestions = exam.numQuestions,
                    language = exam.language,
                    difficulty = exam.difficulty,
                    status = exam.status,
                    createdAt = exam.createdAt
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadingDetail = false,
                    errorMessage = e.message ?: "시험 정보를 불러오지 못했습니다."
                )
            }
        }
    }

    fun loadResult() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingResult = true, errorMessage = null)
            try {
                val response: ExamResultResponse =
                    apiService.getExamResult("Bearer $token", subjectId, examId)

                val totalCount = response.questions.size
                val correctCount = response.questions.count { q ->
                    q.userAnswer != null &&
                            q.correctAnswer != null &&
                            q.userAnswer == q.correctAnswer
                }

                val totalScore = 100.0
                val summaryText = "총 ${totalCount}문제 중 ${correctCount}개 정답, 점수 ${response.score}점"

                _uiState.value = _uiState.value.copy(
                    loadingResult = false,
                    result = response,
                    score = response.score,
                    totalScore = totalScore,
                    correctCount = correctCount,
                    totalCount = totalCount,
                    summary = summaryText
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadingResult = false,
                    errorMessage = e.message ?: "시험 결과를 불러오지 못했습니다."
                )
            }
        }
    }
}

class ExamDetailViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val examId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamDetailViewModel(apiService, token, subjectId, examId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
