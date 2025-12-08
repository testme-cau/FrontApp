package com.example.testme.ui.screens.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.testme.R
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

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext

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
        factory = TakeExamViewModel.TakeExamViewModelFactory(
            apiService, 
            token, 
            subjectId, 
            examId,
            LocalContext.current.resources
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSubmitConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadExam(context)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.take_exam_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadExam(context, forceRefresh = true) },
                        enabled = !uiState.loading && !uiState.submitting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh)
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
                // No windowInsetsPadding(WindowInsets.ime) here, handled by LazyColumn contentPadding
        ) {
            SoftBlobBackground()

            when {
                uiState.loading && uiState.questions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = subjectColor
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.loading_exam_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.loading_exam_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.questions.isEmpty() && !uiState.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.exam_load_empty),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                OutlinedButton(
                                    onClick = { viewModel.loadExam(context, forceRefresh = true) },
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                }

                else -> {
                    val imeInsets = WindowInsets.ime
                    val bottomPadding = imeInsets.asPaddingValues().calculateBottomPadding()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = bottomPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            ExamHeaderInfo(
                                title = uiState.title,
                                metaText = uiState.metaText,
                                subjectColor = subjectColor
                            )
                        }

                        itemsIndexed(uiState.questions) { index, question ->
                            val selectedIndex = uiState.choiceAnswers[index]
                            val textAnswer = uiState.textAnswers[index]

                            QuestionItemCard(
                                index = index + 1,
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

                        // Submit Button as the last item
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { if (!uiState.submitting) showSubmitConfirm = true },
                                enabled = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    disabledContainerColor = Color.Black.copy(alpha = 0.6f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                if (uiState.submitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.action_submit),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp)) // Normal spacer
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                val msg = uiState.errorMessage ?: stringResource(R.string.msg_error_generic)
                LaunchedEffect(uiState.errorMessage) {
                    snackbarHostState.showSnackbar(msg)
                }
            }

            if (showSubmitConfirm) {
                AlertDialog(
                    onDismissRequest = { showSubmitConfirm = false },
                    title = { Text(stringResource(R.string.exam_submit_confirm_title)) },
                    text = { Text(stringResource(R.string.exam_submit_confirm_msg)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitConfirm = false
                                scope.launch {
                                    val result = viewModel.submitExam()
                                    if (result.isSuccess) {
                                        navController.popBackStack()
                                        launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.exam_submitted_success))
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            result.exceptionOrNull()?.message ?: context.getString(R.string.exam_submit_fail)
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = subjectColor)
                        ) {
                            Text(stringResource(R.string.action_submit))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showSubmitConfirm = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                )
            }
        }
    }
}

@Composable
fun ExamHeaderInfo(
    title: String,
    metaText: String?,
    subjectColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            if (!metaText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = metaText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF525252),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionItemCard(
    index: Int,
    question: ExamQuestion,
    selectedOptionIndex: Int?,
    textAnswer: String,
    onOptionSelected: (Int) -> Unit,
    onTextChanged: (String) -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Q1 [Type]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
                
                val typeLabel = when (question.type) {
                    "multiple_choice" -> stringResource(R.string.qtype_multiple_choice)
                    "short_answer" -> stringResource(R.string.qtype_short_answer)
                    "essay" -> stringResource(R.string.qtype_essay)
                    else -> stringResource(R.string.qtype_unknown)
                }
                
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Question Text
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.3f
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Answer Input
            when (question.type) {
                "multiple_choice" -> {
                    MultipleChoiceInput(
                        options = question.options ?: emptyList(),
                        selectedIndex = selectedOptionIndex,
                        onOptionSelected = onOptionSelected,
                        accentColor = accentColor
                    )
                }
                else -> {
                    TextInput(
                        text = textAnswer,
                        onTextChanged = onTextChanged,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MultipleChoiceInput(
    options: List<String>,
    selectedIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    accentColor: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { idx, optionText ->
            val isSelected = selectedIndex == idx
            OptionRow(
                text = optionText,
                isSelected = isSelected,
                onClick = { onOptionSelected(idx) },
                accentColor = accentColor
            )
        }
    }
}

@Composable
fun OptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val borderColor = if (isSelected) accentColor else Color(0xFFE5E7EB)
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.05f) else Color.Transparent
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color(0xFF9CA3AF),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isSelected) Color(0xFF111827) else Color(0xFF4B5563),
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun TextInput(
    text: String,
    onTextChanged: (String) -> Unit,
    accentColor: Color
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.hint_answer_input)) },
        placeholder = { Text(stringResource(R.string.hint_answer_input)) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            focusedLabelColor = accentColor,
            cursorColor = accentColor
        ),
        minLines = 3,
        maxLines = 10
    )
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
    private val examId: String,
    private val resources: Resources
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TakeExamUiState(
            examId = examId,
            subjectId = subjectId
        )
    )
    val uiState: StateFlow<TakeExamUiState> = _uiState

    fun loadExam(context: Context, forceRefresh: Boolean = false) {
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
                metaParts.add(context.getString(R.string.exam_questions_fmt, exam.numQuestions))
                
                if (exam.difficulty != null) {
                    val diff = when (exam.difficulty) {
                        "easy" -> context.getString(R.string.difficulty_easy)
                        "medium" -> context.getString(R.string.difficulty_medium)
                        "hard" -> context.getString(R.string.difficulty_hard)
                        else -> exam.difficulty ?: context.getString(R.string.difficulty_unknown)
                    }
                    metaParts.add(context.getString(R.string.exam_difficulty_fmt, diff))
                }
                
                val lang = exam.language ?: context.getString(R.string.language_unspecified)
                metaParts.add(lang)
                
                val meta = metaParts.joinToString(" · ")

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    title = exam.title ?: context.getString(R.string.exam_new_default),
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
                    errorMessage = e.message ?: context.getString(R.string.exam_load_fail)
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
            return Result.failure(IllegalStateException(resources.getString(R.string.err_no_exam_info_to_submit)))
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
                errorMessage = e.message ?: resources.getString(R.string.exam_submit_fail),
                submitSuccess = false
            )
            Result.failure(e)
        }
    }

    class TakeExamViewModelFactory(
        private val apiService: ApiService,
        private val token: String,
        private val subjectId: String,
        private val examId: String,
        private val resources: Resources
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TakeExamViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TakeExamViewModel(
                    apiService = apiService,
                    token = token,
                    subjectId = subjectId,
                    examId = examId,
                    resources = resources
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
