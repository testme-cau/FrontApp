package com.example.testme.ui.screens.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.ExamDetailResponse
import com.example.testme.data.model.ExamResultResponse
import com.example.testme.data.model.ExamQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.testme.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import kotlinx.coroutines.launch

data class ExamDetailHeaderUi(
    val title: String,
    val scoreLabel: String,           // "10.0 / 100.0점"
    val percentageLabel: String,      // "10.0%"
    val difficultyLabel: String,      // "쉬움/보통/어려움/..."
    val languageLabel: String,        // "ko"/"en"/"언어 미지정"
    val metaLabel: String,            // "문항 5개 · 생성일 ..."
    val overallFeedback: String,      // 없으면 ""
    val strengths: List<String>,
    val weaknesses: List<String>,
    val studyRecommendations: List<String>
)

data class QuestionResultUi(
    val index: Int,
    val questionText: String,
    val typeLabel: String,
    val pointsLabel: String?,         // "배점 13점" (없을 수도)
    val isCorrect: Boolean?,          // 정/오/없음
    val scoreLabel: String?,          // "0 / 13점"
    val userAnswerLabel: String?,     // 내 답
    val correctAnswerLabel: String?,  // 정답
    val modelAnswer: String?,         // 모범답안
    val feedback: String?             // 피드백
)

data class ExamDetailUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val header: ExamDetailHeaderUi? = null,
    val questions: List<QuestionResultUi> = emptyList()
)

class ExamDetailViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val examId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamDetailUiState())
    val uiState: StateFlow<ExamDetailUiState> = _uiState

    fun loadDetail(context: android.content.Context) {
        if (_uiState.value.loading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            try {
                val examDetail: ExamDetailResponse =
                    apiService.getExamDetail("Bearer $token", subjectId, examId)
                val exam = examDetail.exam

                val result: ExamResultResponse =
                    apiService.getExamResult("Bearer $token", subjectId, examId)
                val submission = result.submission
                val grading = submission.gradingResult
                    ?: throw IllegalStateException(context.getString(R.string.msg_no_grading_result))

                val scoreLabel = context.getString(
                    R.string.score_fmt,
                    grading.totalScore,
                    grading.maxScore
                )
                val percentageLabel = context.getString(R.string.percentage_fmt, grading.percentage)

                val difficultyLabel: String = when (exam.difficulty) {
                    "easy" -> context.getString(R.string.difficulty_easy)
                    "medium" -> context.getString(R.string.difficulty_medium)
                    "hard" -> context.getString(R.string.difficulty_hard)
                    else -> exam.difficulty ?: context.getString(R.string.difficulty_unknown)
                }

                val languageLabel: String = exam.language ?: context.getString(R.string.language_unspecified)
                val metaLabel = context.getString(R.string.meta_exam_detail_fmt, exam.numQuestions, exam.createdAt)

                val header = ExamDetailHeaderUi(
                    title = exam.title ?: "",
                    scoreLabel = scoreLabel,
                    percentageLabel = percentageLabel,
                    difficultyLabel = difficultyLabel,
                    languageLabel = languageLabel,
                    metaLabel = metaLabel,
                    overallFeedback = grading.overallFeedback.orEmpty(),
                    strengths = grading.strengths,
                    weaknesses = grading.weaknesses,
                    studyRecommendations = grading.studyRecommendations
                )

                // ---------- 문항별 ----------
                val answersByQuestionId = submission.answers.associateBy { it.questionId }
                val questionsById = exam.questions.associateBy { it.id }

                val questionResults = grading.questionResults.mapIndexed { idx, qr ->
                    val qDetail: ExamQuestion? = questionsById[qr.questionId]

                    val typeLabel = when (qDetail?.type) {
                        "multiple_choice" -> context.getString(R.string.qtype_multiple_choice)
                        "short_answer" -> context.getString(R.string.qtype_short_answer)
                        "essay" -> context.getString(R.string.qtype_essay)
                        else -> qDetail?.type ?: context.getString(R.string.qtype_unknown)
                    }

                    val maxPoints = qr.maxPoints ?: qDetail?.points
                    val pointsLabel = maxPoints?.let { context.getString(R.string.label_points_fmt, it.toInt()) }

                    val options = qDetail?.options ?: emptyList()

                    // 제출 답
                    val rawUserAnswer: String? = answersByQuestionId[qr.questionId]?.answer
                    val userAnswerLabel: String? = if (rawUserAnswer != null && options.isNotEmpty()) {
                        val idxOpt = rawUserAnswer.toIntOrNull()
                        if (idxOpt != null && idxOpt in options.indices) {
                            options[idxOpt]
                        } else {
                            rawUserAnswer
                        }
                    } else {
                        rawUserAnswer
                    }

                    // 정답 텍스트
                    val correctAnswerLabel: String? = when {
                        qDetail?.correctAnswer != null && options.isNotEmpty() -> {
                            val idxOpt = qDetail.correctAnswer.toIntOrNull()
                            if (idxOpt != null && idxOpt in options.indices) {
                                options[idxOpt]
                            } else {
                                qDetail.correctAnswer
                            }
                        }
                        qDetail?.correctAnswer != null -> qDetail.correctAnswer
                        else -> null
                    }

                    val scoreLabelPerQuestion: String? =
                        if (qr.score != null && maxPoints != null) {
                            context.getString(R.string.score_simple_fmt, qr.score.toInt(), maxPoints.toInt())
                        } else null

                    QuestionResultUi(
                        index = idx + 1,
                        questionText = qDetail?.question ?: context.getString(R.string.msg_question_load_fail),
                        typeLabel = typeLabel,
                        pointsLabel = pointsLabel,
                        isCorrect = qr.isCorrect,
                        scoreLabel = scoreLabelPerQuestion,
                        userAnswerLabel = userAnswerLabel,
                        correctAnswerLabel = correctAnswerLabel,
                        modelAnswer = (qr.modelAnswer ?: qDetail?.modelAnswer),
                        feedback = qr.feedback
                    )
                }

                _uiState.value = ExamDetailUiState(
                    loading = false,
                    errorMessage = null,
                    header = header,
                    questions = questionResults
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: context.getString(R.string.msg_load_result_fail)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    examId: String,
    subjectColor: Color = MaterialTheme.colorScheme.primary,
    viewModel: ExamDetailViewModel = viewModel(
        factory = ExamDetailViewModelFactory(apiService, token, subjectId, examId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadDetail(context)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(message = msg)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.title_exam_result),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SoftBlobBackground()

            when {
                uiState.loading && uiState.header == null -> {
                    // 처음 로딩일 때는 중앙 로딩 카드
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = subjectColor
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.msg_loading_result),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.msg_loading_result_desc),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.header == null && uiState.errorMessage != null -> {
                    // 치명적 에러
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                uiState.errorMessage ?: stringResource(R.string.msg_load_result_fail),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(
                                onClick = { viewModel.loadDetail(context) },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }

                uiState.header == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.msg_no_result_display))
                    }
                }

                else -> {
                    val header = uiState.header!!

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            ExamResultSummaryCard(
                                header = header,
                                accentColor = subjectColor
                            )
                        }

                        items(uiState.questions) { q ->
                            QuestionResultCard(
                                result = q,
                                accentColor = subjectColor
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    if (uiState.loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = subjectColor.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamResultSummaryCard(
    header: ExamDetailHeaderUi,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = header.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F241B)
                    )
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = header.scoreLabel,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E4032)
                        )
                    )
                    ResultPill(
                        label = header.percentageLabel,
                        color = accentColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallChip("난이도 ${header.difficultyLabel}")
                    SmallChip(header.languageLabel)
                }

                Text(
                    text = header.metaLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF1E4032).copy(alpha = 0.75f)
                    )
                )

                if (header.overallFeedback.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.label_overall_feedback), accentColor)
                    Text(
                        text = header.overallFeedback,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (header.strengths.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.label_strengths), accentColor)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        header.strengths.forEach { s ->
                            Text("• $s", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (header.weaknesses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.label_weaknesses), accentColor)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        header.weaknesses.forEach { w ->
                            Text("• $w", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (header.studyRecommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.label_study_recommendations), accentColor)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        header.studyRecommendations.forEach { r ->
                            Text("• $r", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    )
}

@Composable
private fun SmallChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color(0xFF1E4032).copy(alpha = 0.08f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun QuestionResultCard(
    result: QuestionResultUi,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
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
                Column {
                    Text(
                        text = stringResource(R.string.label_question_fmt, result.index, result.typeLabel),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    result.pointsLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF1E4032).copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                result.isCorrect?.let { isCorrect ->
                    val label = if (isCorrect) stringResource(R.string.label_correct) else stringResource(R.string.label_incorrect)
                    val color = if (isCorrect) accentColor else MaterialTheme.colorScheme.error

                    Box(
                        modifier = Modifier
                            .background(
                                color.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        )
                    }
                }
            }

            Text(
                text = result.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            result.scoreLabel?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E4032)
                    )
                )
            }

            result.userAnswerLabel?.let {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.label_my_answer), accentColor)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            result.correctAnswerLabel?.let {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.label_correct_answer), accentColor)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            result.modelAnswer?.let {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.label_model_answer), accentColor)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            result.feedback?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_feedback),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
