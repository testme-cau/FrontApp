package com.example.testme.ui.screens.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.testme.data.model.ExamSubmitResponse
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
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
    subjectColor: Color = MaterialTheme.colorScheme.primary,
    viewModel: TakeExamViewModel = viewModel(
        factory = TakeExamViewModel.TakeExamViewModelFactory(apiService, token, subjectId, examId)
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
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = uiState.title.ifBlank { "시험 응시" },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadExam(forceRefresh = true) },
                        enabled = !uiState.loading && !uiState.submitting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SoftBlobBackground()

            when {
                uiState.loading && uiState.questions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp),
                                    strokeWidth = 3.dp,
                                    color = subjectColor
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "시험지 불러오는 중...",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "AI가 문제를 정리하고 있어요.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.questions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("불러온 문제가 없습니다.")
                                OutlinedButton(onClick = { viewModel.loadExam(forceRefresh = true) }) {
                                    Text("다시 시도")
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                color = Color(0xFF0F241B)
                            )
                        )
                        if (!uiState.metaText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.metaText.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF1E4032).copy(alpha = 0.8f)
                                )
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
                                val selectedIndex = uiState.choiceAnswers[index]
                                val textAnswer = uiState.textAnswers[index]

                                QuestionCard(
                                    number = index + 1,
                                    question = question,
                                    selectedOptionIndex = selectedIndex,
                                    textAnswer = textAnswer.orEmpty(),
                                    onOptionSelected = { optionIdx ->
                                        viewModel.selectChoiceAnswer(index, optionIdx)
                                    },
                                    onTextChanged = { text ->
                                        viewModel.updateTextAnswer(index, text)
                                    },
                                    accentColor = subjectColor
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
                                        navController.popBackStack()
                                        launch {
                                            snackbarHostState.showSnackbar("시험이 제출되었습니다. 채점이 진행됩니다.")
                                        }
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
}

@Composable
private fun QuestionCard(
    number: Int,
    question: ExamQuestion,
    selectedOptionIndex: Int?,
    textAnswer: String,
    onOptionSelected: (Int) -> Unit,
    onTextChanged: (String) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeLabel = when (question.type) {
                "multiple_choice" -> "객관식"
                "short_answer" -> "단답형"
                "essay" -> "서술형"
                else -> "주관식"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Q$number",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF0F241B)
                    )
                )
                Text(
                    text = "[$typeLabel]",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accentColor
                    )
                )
            }

            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            )

            when (question.type) {
                "multiple_choice" -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        question.options?.forEachIndexed { idx, optionText ->
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

                "short_answer" -> {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("답변을 입력하세요") },
                        singleLine = false
                    )
                }

                else -> {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("답변을 입력하세요") },
                        singleLine = false
                    )
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
    val choiceAnswers: Map<Int, Int> = emptyMap(),
    val textAnswers: Map<Int, String> = emptyMap(),
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
            _uiState.value = _uiState.value.copy(
                loading = true,
                errorMessage = null
            )
            try {
                val res: ExamDetailResponse =
                    apiService.getExamDetail("Bearer $token", subjectId, examId)

                val exam = res.exam
                val questions = exam.questions

                val metaParts = mutableListOf<String>()
                metaParts.add("문항 수: ${exam.numQuestions}")
                metaParts.add("난이도: ${exam.difficulty}")
                exam.language?.let { metaParts.add("언어: $it") }
                val mcCount = questions.count { it.type == "multiple_choice" }
                val saCount = questions.count { it.type == "short_answer" }
                val essayCount = questions.count { it.type == "essay" }

                val typeParts = mutableListOf<String>()
                if (mcCount > 0) typeParts.add("객관식 $mcCount")
                if (saCount > 0) typeParts.add("단답형 $saCount")
                if (essayCount > 0) typeParts.add("서술형 $essayCount")
                if (typeParts.isNotEmpty()) {
                    metaParts.add(typeParts.joinToString(", "))
                }

                val meta = metaParts.joinToString(" • ")

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = exam.title,
                    metaText = meta,
                    questions = questions,
                    choiceAnswers = emptyMap(),
                    textAnswers = emptyMap(),
                    submitSuccess = false,
                    lastSubmissionId = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "시험 정보를 불러오지 못했습니다."
                )
            }
        }
    }

    fun selectChoiceAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _uiState.value
        val newMap = current.choiceAnswers.toMutableMap()
        newMap[questionIndex] = optionIndex
        _uiState.value = current.copy(choiceAnswers = newMap)
    }

    fun updateTextAnswer(questionIndex: Int, text: String) {
        val current = _uiState.value
        val newMap = current.textAnswers.toMutableMap()
        newMap[questionIndex] = text
        _uiState.value = current.copy(textAnswers = newMap)
    }

    suspend fun submitExam(): Result<ExamSubmitResponse> {
        val state = _uiState.value
        if (state.questions.isEmpty()) {
            return Result.failure(IllegalStateException("제출할 시험 정보가 없습니다."))
        }

        _uiState.value = state.copy(
            submitting = true,
            errorMessage = null,
            submitSuccess = false
        )

        return try {
            val payloads = state.questions.mapIndexed { idx, q ->
                val answerStr = when (q.type) {
                    "multiple_choice" -> {
                        val selectedIndex = state.choiceAnswers[idx]
                        val selectedText = if (selectedIndex != null) {
                            q.options?.getOrNull(selectedIndex)
                        } else null

                        selectedText?.takeIf { it.isNotBlank() } ?: "[NO_ANSWER]"
                    }

                    "short_answer" -> {
                        val text = state.textAnswers[idx]?.trim()
                        if (text.isNullOrEmpty()) "[NO_ANSWER]" else text
                    }

                    else -> {
                        val text = state.textAnswers[idx]?.trim()
                        if (text.isNullOrEmpty()) "[NO_ANSWER]" else text
                    }
                }

                ExamAnswerPayload(
                    questionId = q.id,
                    answer = answerStr
                )
            }

            val res = apiService.submitExam(
                token = "Bearer $token",
                subjectId = subjectId,
                examId = examId,
                body = payloads
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
}
