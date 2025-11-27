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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.ExamAnswerPayload
import com.example.testme.data.model.ExamDetailResponse
import com.example.testme.data.model.ExamQuestion
import com.example.testme.data.model.ExamSubmitRequest
import com.example.testme.data.model.ExamSubmitResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeExamScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    examId: String,
    viewModel: TakeExamViewModel = viewModel(
        factory = TakeExamViewModelFactory(apiService, token, subjectId, examId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSubmitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadExam()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title.ifBlank { "시험 응시" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadExam(forceRefresh = true) },
                        enabled = !uiState.loading && !uiState.submitting
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            uiState.loading && uiState.questions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.questions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("불러온 문제가 없습니다.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadExam(forceRefresh = true) }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!uiState.metaText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.metaText ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(uiState.questions) { index, question ->
                            val selectedIndex = uiState.answers[question.index]
                            QuestionCard(
                                number = index + 1,
                                question = question,
                                selectedOptionIndex = selectedIndex,
                                onOptionSelected = { optionIdx ->
                                    viewModel.selectAnswer(question.index, optionIdx)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }

                    Button(
                        onClick = { showSubmitConfirm = true },
                        enabled = !uiState.submitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (uiState.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(18.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(if (uiState.submitting) "제출 중..." else "시험 제출")
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            LaunchedEffect(uiState.errorMessage) {
                snackbarHostState.showSnackbar(uiState.errorMessage ?: "오류가 발생했습니다.")
            }
        }

        if (uiState.submitSuccess && uiState.lastSubmissionId != null) {
            LaunchedEffect(uiState.lastSubmissionId) {
                snackbarHostState.showSnackbar("시험이 제출되었습니다. 채점이 진행됩니다.")
            }
        }

        if (showSubmitConfirm) {
            AlertDialog(
                onDismissRequest = { showSubmitConfirm = false },
                title = { Text("시험 제출") },
                text = {
                    Text("정말로 시험을 제출하시겠습니까? 제출 후에는 답안을 수정할 수 없습니다.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitConfirm = false
                            scope.launch {
                                val result = viewModel.submitExam()
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("시험이 제출되었습니다.")
                                    navController.popBackStack()
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.exceptionOrNull()?.message
                                            ?: "시험 제출에 실패했습니다."
                                    )
                                }
                            }
                        }
                    ) {
                        Text("제출")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSubmitConfirm = false }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuestionCard(
    number: Int,
    question: ExamQuestion,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$number. ${question.question}",
                style = MaterialTheme.typography.bodyLarge
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                question.options.forEachIndexed { idx, optionText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(idx) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOptionIndex == idx,
                            onClick = { onOptionSelected(idx) }
                        )
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

data class TakeExamUiState(
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val examId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val metaText: String? = null,
    val questions: List<ExamQuestion> = emptyList(),
    val answers: Map<Int, Int> = emptyMap(),
    val errorMessage: String? = null,
    val submitSuccess: Boolean = false,
    val lastSubmissionId: String? = null
)

class TakeExamViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val examId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TakeExamUiState(
            examId = examId,
            subjectId = subjectId
        )
    )
    val uiState: StateFlow<TakeExamUiState> = _uiState

    fun loadExam(forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (current.loading && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = current.copy(loading = true, errorMessage = null)
            try {
                val res: ExamDetailResponse =
                    apiService.getExamDetail("Bearer $token", subjectId, examId)
                val exam = res.exam
                val questions = res.questions
                val meta = buildString {
                    append("문항 수: ${exam.numQuestions}")
                    append(" • 난이도: ${exam.difficulty}")
                    append(" • 언어: ${exam.language}")
                }

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = exam.title,
                    metaText = meta,
                    questions = questions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "시험 정보를 불러오지 못했습니다."
                )
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _uiState.value
        val newMap = current.answers.toMutableMap()
        newMap[questionIndex] = optionIndex
        _uiState.value = current.copy(answers = newMap)
    }

    suspend fun submitExam(): Result<ExamSubmitResponse> {
        val state = _uiState.value
        if (state.questions.isEmpty()) {
            return Result.failure(IllegalStateException("제출할 시험 정보가 없습니다."))
        }
        _uiState.value = state.copy(submitting = true, errorMessage = null, submitSuccess = false)
        return try {
            val payloads = state.questions.map { q ->
                val qid = q.questionId ?: q.index
                val selected = state.answers[q.index]
                val answerStr = selected?.toString() ?: ""
                ExamAnswerPayload(
                    questionId = qid,
                    answer = answerStr
                )
            }
            val body = ExamSubmitRequest(answers = payloads)
            val res = apiService.submitExam(
                token = "Bearer $token",
                subjectId = subjectId,
                examId = examId,
                body = body
            )
            _uiState.value = _uiState.value.copy(
                submitting = false,
                submitSuccess = true,
                lastSubmissionId = res.submissionId
            )
            Result.success(res)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                submitting = false,
                errorMessage = e.message ?: "시험 제출에 실패했습니다.",
                submitSuccess = false
            )
            Result.failure(e)
        }
    }
}

class TakeExamViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val examId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TakeExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TakeExamViewModel(
                apiService = apiService,
                token = token,
                subjectId = subjectId,
                examId = examId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
