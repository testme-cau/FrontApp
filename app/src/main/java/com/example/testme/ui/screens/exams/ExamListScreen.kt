package com.example.testme.ui.screens.exam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.testme.data.model.ExamListResponse
import com.example.testme.data.model.JobResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    subjectName: String? = null,
    viewModel: ExamListViewModel = viewModel(
        factory = ExamListViewModelFactory(apiService, token, subjectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var examToDelete by remember { mutableStateOf<ExamSummaryUi?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadExams()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subjectName?.let { "시험 목록 · $it" } ?: "시험 목록"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadExams(forceRefresh = true) },
                        enabled = !uiState.loading
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("subjects/$subjectId/generate-exam")
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "시험 생성")
            }
        }
    ) { padding ->
        when {
            uiState.loading && uiState.exams.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.exams.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("이 과목에는 아직 생성된 시험이 없습니다.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                navController.navigate("subjects/$subjectId/generate-exam")
                            }
                        ) {
                            Text("첫 시험 만들기")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.exams) { exam ->
                        ExamItemCard(
                            exam = exam,
                            onClick = {
                                navController.navigate("subjects/$subjectId/exams/${exam.examId}")
                            },
                            onDelete = {
                                examToDelete = exam
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            LaunchedEffect(uiState.errorMessage) {
                snackbarHostState.showSnackbar(uiState.errorMessage ?: "오류가 발생했습니다.")
            }
        }

        if (examToDelete != null) {
            AlertDialog(
                onDismissRequest = { examToDelete = null },
                title = { Text("시험 삭제") },
                text = {
                    Text(
                        "\"${examToDelete?.title ?: examToDelete?.shortLabel}\" 시험을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = examToDelete
                            if (target != null) {
                                scope.launch {
                                    val result = viewModel.deleteExam(target.examId)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("시험이 삭제되었습니다.")
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            result.exceptionOrNull()?.message
                                                ?: "시험 삭제에 실패했습니다."
                                        )
                                    }
                                    examToDelete = null
                                }
                            } else {
                                examToDelete = null
                            }
                        }
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { examToDelete = null }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExamItemCard(
    exam: ExamSummaryUi,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title.ifBlank { exam.shortLabel },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = exam.subtitle,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = exam.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = exam.statusColor
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "시험 삭제"
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "문항 ${exam.numQuestions}개",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "난이도 ${exam.difficultyLabel}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = exam.languageLabel,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (!exam.createdAtLabel.isNullOrBlank()) {
                Text(
                    text = exam.createdAtLabel ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

data class ExamSummaryUi(
    val examId: String,
    val title: String,
    val numQuestions: Int,
    val language: String?,
    val difficulty: String?,
    val status: String?,
    val createdAt: String?
) {
    val shortLabel: String
        get() = "시험 #${examId.takeLast(6)}"

    val subtitle: String
        get() = "ID: $examId"

    val statusLabel: String
        get() = when (status) {
            "draft" -> "초안"
            "ready" -> "준비 완료"
            "in_progress" -> "진행 중"
            "completed" -> "완료"
            else -> status ?: "상태 미정"
        }

    val statusColor: Color
        @Composable
        get() = when (status) {
            "completed" -> MaterialTheme.colorScheme.primary
            "in_progress" -> MaterialTheme.colorScheme.tertiary
            "draft" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    val difficultyLabel: String
        get() = when (difficulty) {
            "easy" -> "쉬움"
            "medium" -> "보통"
            "hard" -> "어려움"
            else -> difficulty ?: "알 수 없음"
        }

    val languageLabel: String
        get() = language ?: "언어 미지정"

    val createdAtLabel: String?
        get() = createdAt
}

data class ExamListUiState(
    val exams: List<ExamSummaryUi> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null
)

class ExamListViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamListUiState())
    val uiState: StateFlow<ExamListUiState> = _uiState

    fun loadExams(forceRefresh: Boolean = false) {
        if (_uiState.value.loading && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            try {
                val response: ExamListResponse =
                    apiService.getExamsBySubject("Bearer $token", subjectId)

                val items = response.exams.map { exam ->
                    ExamSummaryUi(
                        examId = exam.examId,
                        title = exam.title ?: "",
                        numQuestions = exam.numQuestions ?: 0,
                        language = exam.language ?: null,
                        difficulty = exam.difficulty ?: null,
                        status = exam.status ?: null,
                        createdAt = exam.createdAt ?: null
                    )
                }

                _uiState.value = _uiState.value.copy(
                    exams = items,
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "시험 목록을 불러오지 못했습니다."
                )
            }
        }
    }

    suspend fun deleteExam(examId: String): Result<JobResponse> {
        return try {
            val response = apiService.deleteExam("Bearer $token", subjectId, examId)
            loadExams(forceRefresh = true)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ExamListViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamListViewModel(apiService, token, subjectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
