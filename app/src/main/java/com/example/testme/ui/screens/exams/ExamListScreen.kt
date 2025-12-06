package com.example.testme.ui.screens.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.testme.R
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.ExamListResponse
import com.example.testme.ui.navigation.Screen
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    subjectName: String? = null,
    subjectColor: Color = MaterialTheme.colorScheme.primary,
    viewModel: ExamListViewModel = viewModel(
        factory = ExamListViewModelFactory(apiService, token, subjectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var examToDelete by remember { mutableStateOf<ExamSummaryUi?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadExams()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TestMeTopAppBar(
                title = subjectName?.let { stringResource(R.string.title_exam_list_fmt, it) } ?: stringResource(R.string.exam_list_title),
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
                        onClick = { viewModel.loadExams(forceRefresh = true) },
                        enabled = !uiState.loading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh)
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("subjects/$subjectId/generate-exam")
                },
                containerColor = subjectColor,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.action_generate_exam))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SoftBlobBackground()

            when {
                uiState.loading && uiState.exams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 16.dp
                                ),
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
                                        text = stringResource(R.string.msg_exam_list_loading),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.msg_exam_list_loading_desc),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.exams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.msg_exam_list_empty),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Button(
                                    onClick = {
                                        navController.navigate("subjects/$subjectId/generate-exam")
                                    }
                                ) {
                                    Text(stringResource(R.string.action_create_first_exam))
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.exams) { exam ->
                            val primaryNav: () -> Unit = when {
                                exam.canViewResult -> {
                                    { navController.navigate(Screen.ExamDetail.route(subjectId, exam.examId)) }
                                }

                                exam.canTakeExam -> {
                                    { navController.navigate(Screen.TakeExam.route(subjectId, exam.examId)) }
                                }

                                else -> {
                                    {}
                                }
                            }

                            ExamItemCard(
                                exam = exam,
                                onClick = primaryNav,
                                onTakeExam = {
                                    navController.navigate(Screen.TakeExam.route(subjectId, exam.examId))
                                },
                                onViewResult = {
                                    navController.navigate(
                                        Screen.ExamDetail.route(
                                            subjectId,
                                            exam.examId
                                        )
                                    )
                                },
                                onDelete = { examToDelete = exam },
                                accentColor = subjectColor
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }

            if (uiState.loading && uiState.exams.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = subjectColor.copy(alpha = 0.9f)
                    )
                }
            }

            if (examToDelete != null) {
                AlertDialog(
                    onDismissRequest = { examToDelete = null },
                    title = { Text(stringResource(R.string.exam_delete_title)) },
                    text = {
                        Text(
                            stringResource(R.string.exam_delete_confirm, examToDelete?.title ?: examToDelete?.shortLabel ?: "")
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
                                            snackbarHostState.showSnackbar(context.getString(R.string.exam_delete_success))
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                result.exceptionOrNull()?.message
                                                    ?: context.getString(R.string.exam_delete_fail)
                                            )
                                        }
                                        examToDelete = null
                                    }
                                } else {
                                    examToDelete = null
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_delete))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { examToDelete = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExamItemCard(
    exam: ExamSummaryUi,
    onClick: () -> Unit,
    onTakeExam: () -> Unit,
    onViewResult: () -> Unit,
    onDelete: () -> Unit,
    accentColor: Color
) {
    val (primaryLabel, primaryAction, primaryEnabled) = when {
        exam.canViewResult -> Triple(stringResource(R.string.action_view_result), onViewResult, true)
        exam.canTakeExam -> Triple(stringResource(R.string.action_take_exam), onTakeExam, true)
        else -> Triple(stringResource(R.string.status_grading), {}, false)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (primaryEnabled) {
                    base.clickable(onClick = onClick)
                } else {
                    base
                }
            },
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (primaryEnabled) primaryAction() },
                                enabled = primaryEnabled
                            ) {
                                Text(primaryLabel)
                            }
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete)
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exam_questions_fmt, exam.numQuestions),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = stringResource(R.string.exam_difficulty_fmt, exam.difficultyLabel),
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
}

data class ExamSummaryUi(
    val examId: String,
    val title: String,
    val numQuestions: Int,
    val language: String?,
    val difficulty: String?,
    val status: String?,
    val createdAt: String?,
    val jobStatus: String?
) {
    val shortLabel: String
        get() = "Exam #${examId.takeLast(6)}"

    val subtitle: String
        get() = "ID: $examId"

    val statusLabel: String
        @Composable
        get() = when (jobStatus) {
            "processing" -> stringResource(R.string.status_processing)
            "completed" -> stringResource(R.string.status_grading_completed)
            else -> when (status) {
                "draft" -> stringResource(R.string.status_draft)
                "ready" -> stringResource(R.string.status_ready)
                "in_progress" -> stringResource(R.string.status_in_progress)
                "active" -> stringResource(R.string.status_active)
                else -> status ?: stringResource(R.string.status_unknown)
            }
        }

    val statusColor: Color
        @Composable
        get() = when (jobStatus) {
            "completed" -> MaterialTheme.colorScheme.primary
            "processing" -> MaterialTheme.colorScheme.tertiary
            else -> when (status) {
                "active", "ready" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

    val difficultyLabel: String
        @Composable
        get() = when (difficulty) {
            "easy" -> stringResource(R.string.difficulty_easy)
            "medium" -> stringResource(R.string.difficulty_medium)
            "hard" -> stringResource(R.string.difficulty_hard)
            else -> difficulty ?: stringResource(R.string.difficulty_unknown)
        }

    val languageLabel: String
        @Composable
        get() = language ?: stringResource(R.string.language_unspecified)

    val createdAtLabel: String?
        get() = createdAt

    val canViewResult: Boolean
        get() = jobStatus == "completed"

    val canTakeExam: Boolean
        get() = jobStatus == null
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
                val examResponse: ExamListResponse =
                    apiService.getExamsBySubject("Bearer $token", subjectId)

                val gradingJobsResponse =
                    apiService.getGradingJobs("Bearer $token", subjectId)

                val jobByExamId: Map<String, String?> =
                    gradingJobsResponse.jobs
                        .sortedByDescending { it.createdAt }
                        .groupBy { it.examId ?: "" }
                        .mapValues { (_, jobs) -> jobs.firstOrNull()?.status }

                val items = examResponse.exams.map { exam ->
                    val jobStatusForExam = jobByExamId[exam.examId]
                    ExamSummaryUi(
                        examId = exam.examId,
                        title = exam.title ?: "",
                        numQuestions = exam.numQuestions ?: 0,
                        language = exam.language,
                        difficulty = exam.difficulty,
                        status = exam.status,
                        createdAt = exam.createdAt,
                        jobStatus = jobStatusForExam
                    )
                }

                _uiState.value = _uiState.value.copy(
                    exams = items,
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "Failed to load exams"
                )
            }
        }
    }

    suspend fun deleteExam(examId: String): Result<Unit> {
        return try {
            val res = apiService.deleteExam("Bearer $token", subjectId, examId)
            if (res.isSuccessful) {
                loadExams(forceRefresh = true)
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("HTTP ${res.code()} ${res.message()}")
                )
            }
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
