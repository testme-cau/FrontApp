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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private fun formatDateTime(context: android.content.Context, raw: String?): String {
        if (raw.isNullOrBlank()) return context.getString(R.string.unknown)
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.getDefault())
        val formatted = runCatching {
            Instant.parse(raw).atZone(ZoneId.systemDefault()).format(formatter)
        }.getOrNull()
        return formatted ?: raw
    }

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
                val formattedCreatedAt = formatDateTime(context, exam.createdAt)
                val metaLabel = context.getString(R.string.meta_exam_detail_fmt, exam.numQuestions, formattedCreatedAt)

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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = header.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallChip(stringResource(R.string.label_difficulty_with_value, header.difficultyLabel))
                SmallChip(stringResource(R.string.label_language_with_value, header.languageLabel))
            }
            Text(
                text = header.metaLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            )
        }

        StatCard(
            title = stringResource(R.string.label_total_score_with_pct, header.percentageLabel),
            value = header.scoreLabel,
            accentColor = accentColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.label_ai_report), Color.Black)

            // Overview Card
            HighlightCard(
                title = stringResource(R.string.label_overview),
                items = listOf(header.overallFeedback.ifBlank { stringResource(R.string.msg_no_result_display) }),
                leadingIcon = Icons.Filled.Info,
                containerColor = Color(0xFFEFF6FF), // Light Blue
                borderColor = Color(0xFFDBEAFE),
                accentTextColor = Color(0xFF1E3A8A),
                isBulleted = false
            )

            HighlightCard(
                title = stringResource(R.string.label_strengths),
                items = header.strengths,
                leadingIcon = Icons.Filled.CheckCircle,
                containerColor = Color(0xFFF0FDF4), // Light Green
                borderColor = Color(0xFFDCFCE7),
                accentTextColor = Color(0xFF166534)
            )
            HighlightCard(
                title = stringResource(R.string.label_weaknesses),
                items = header.weaknesses,
                leadingIcon = Icons.Filled.Cancel,
                containerColor = Color(0xFFFEF2F2), // Light Red
                borderColor = Color(0xFFFEE2E2),
                accentTextColor = Color(0xFF991B1B)
            )
            HighlightCard(
                title = stringResource(R.string.label_learning_guide),
                items = header.studyRecommendations,
                leadingIcon = Icons.Filled.Lightbulb,
                containerColor = Color(0xFFEFF6FF), // Light Blue/Indigo
                borderColor = Color(0xFFDBEAFE),
                accentTextColor = Color(0xFF1E40AF)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7FAFF))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun HighlightCard(
    title: String,
    items: List<String>,
    leadingIcon: ImageVector,
    containerColor: Color,
    borderColor: Color,
    accentTextColor: Color = MaterialTheme.colorScheme.onSurface,
    isBulleted: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = accentTextColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentTextColor
                    )
                )
            }

            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_no_result_display),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { item ->
                        val text = if (isBulleted) "• $item" else item
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                            )
                        )
                    }
                }
            }
        }
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
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun ScoreBadge(
    text: String,
    accentColor: Color,
    isCorrect: Boolean,
    containerColor: Color,
    textColor: Color = Color.Black
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Color.Transparent) // No border for score badge in reference
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy( // Slightly smaller font
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
private fun ScoreText(
    text: String,
    isCorrect: Boolean?
) {
    val textColor = when (isCorrect) {
        true -> Color(0xFF166534) // Green
        false -> Color(0xFF991B1B) // Red
        else -> Color(0xFFD97706) // Orange (Partial/Unknown)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    )
}

@Composable
private fun AnswerSection(
    label: String,
    content: String?,
    indicatorColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp) // Spacing between bar and text
    ) {
        // Vertical Indicator Bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(IntrinsicSize.Min) // Will not work directly in Row, height needs constraints.
                // Instead, let's use a trick or fixed height logic.
                // Actually, if we want it to stretch with content, we need IntrinsicSize.Min on the Row.
                // But simplified: just a box that spans the height of the content?
                // Compose Row doesn't support "match parent height" easily without intrinsics.
                // Let's use a simpler approach:
                // A Column for text, and draw behind or a box alongside.
                // Let's try IntrinsicSize.Min on Row.
        )
        
        // Correct implementation using Intrinsic measurements
    }
}

@Composable
private fun LabeledTextWithBar(
    label: String,
    value: String?,
    indicatorColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // Allows children to match height
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Indicator Bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(indicatorColor)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy( // Label is small
                    fontWeight = FontWeight.Medium,
                    color = if (label == stringResource(R.string.label_my_answer)) Color.Gray else indicatorColor // "My Answer" label is gray in reference
                )
            )
            Text(
                text = value ?: stringResource(R.string.label_no_answer),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f // Comfortable reading line height
                )
            )
        }
    }
}

@Composable
private fun QuestionResultCard(
    result: QuestionResultUi,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(true) } // Default expanded as per reference showing "Collapse"
    val borderColor = Color(0xFFE5E7EB) // Light gray border

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Reduced corner radius
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(20.dp), // Generous padding
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.label_question_simple_fmt, result.index),
                    style = MaterialTheme.typography.titleLarge.copy( // Larger title
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                result.scoreLabel?.let {
                    ScoreText(text = it, isCorrect = result.isCorrect)
                }
            }

            // Question Text
            SelectionContainer {
                Text(
                    text = result.questionText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF374151), // Dark gray text
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            // My Answer Section
            SelectionContainer {
                LabeledTextWithBar(
                    label = stringResource(R.string.label_my_answer),
                    value = result.userAnswerLabel,
                    indicatorColor = Color(0xFFD1D5DB) // Gray bar
                )
            }

            // Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isExpanded) stringResource(R.string.action_collapse_feedback) else stringResource(R.string.action_expand_feedback),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }

            // Expandable Section (Model Answer & Feedback)
            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    result.modelAnswer?.let {
                        SelectionContainer {
                            LabeledTextWithBar(
                                label = stringResource(R.string.label_model_answer),
                                value = it,
                                indicatorColor = Color(0xFF10B981) // Green bar
                            )
                        }
                    }

                    result.feedback?.let {
                        SelectionContainer {
                            LabeledTextWithBar(
                                label = stringResource(R.string.label_feedback),
                                value = it,
                                indicatorColor = Color(0xFF3B82F6) // Blue bar
                            )
                        }
                    }
                }
            }
        }
    }
}
