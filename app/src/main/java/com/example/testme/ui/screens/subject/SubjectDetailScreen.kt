package com.example.testme.ui.screens.subject

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.ExamListResponse
import com.example.testme.data.model.JobData
import com.example.testme.data.model.JobListResponse
import com.example.testme.data.model.PdfData
import com.example.testme.data.model.PdfDeleteResponse
import com.example.testme.data.model.PdfListResponse
import com.example.testme.data.model.SubjectResponse
import com.example.testme.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.example.testme.R
import com.example.testme.ui.components.SoftBlobBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush


private enum class SubjectDetailTab {
    EXAMS, PDF
}

data class SubjectDetailUiState(
    val loadingSubject: Boolean = false,
    val subjectName: String = "",
    val subjectDescription: String? = null,
    val groupName: String? = null,
    val colorHex: String? = null,

    // PDF
    val loadingPdfs: Boolean = false,
    val pdfs: List<PdfData> = emptyList(),
    val uploading: Boolean = false,
    val uploadProgress: Pair<Int, Int>? = null,
    val deletingFileId: String? = null,

    // 작업(Exam/Grading Job)
    val loadingJobs: Boolean = false,
    val examJobs: List<JobData> = emptyList(),
    val gradingJobs: List<JobData> = emptyList(),

    // 시험 리스트
    val loadingExams: Boolean = false,
    val exams: List<SubjectExamUi> = emptyList(),
    val examsError: String? = null,
    val deletingExamId: String? = null
)

data class SubjectExamUi(
    val examId: String,
    val title: String,
    val numQuestions: Int,
    val language: String?,
    val difficulty: String?,
    val status: String?,
    val createdAt: String?,
    val generationJobStatus: String?,
    val generationProgress: Double?,
    val gradingJobStatus: String?,
    val gradingProgress: Double?
) {
    val shortLabel: String
        get() = "Exam #${examId.takeLast(6)}"

    val activeJobLabel: String?
        get() = when {
            generationJobStatus == "processing" || generationJobStatus == "pending" -> "Generating..."
            gradingJobStatus == "processing" || gradingJobStatus == "pending" -> "Grading..."
            else -> null
        }
    
    val progress: Double?
        get() = when {
            generationJobStatus == "processing" || generationJobStatus == "pending" -> generationProgress
            gradingJobStatus == "processing" || gradingJobStatus == "pending" -> gradingProgress
            else -> null
        }

    val hasOngoingJob: Boolean
        get() = activeJobLabel != null

    val canViewResult: Boolean
        get() = gradingJobStatus == "completed"

    val canTakeExam: Boolean
        get() = status == "active" || status == "ready"
}

class SubjectDetailViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState
    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    fun loadAll() {
        loadSubject()
        loadPdfs()
        loadExams()
        loadJobs()
    }

    private fun hasOngoingJobs(state: SubjectDetailUiState = _uiState.value): Boolean {
        val examJobsOngoing = state.examJobs.any {
            it.status == "processing" || it.status == "pending"
        }
        val gradingJobsOngoing = state.gradingJobs.any {
            it.status == "processing" || it.status == "pending"
        }
        return examJobsOngoing || gradingJobsOngoing
    }

    private fun ensureAutoRefresh() {
        val state = _uiState.value
        val hasOngoing = hasOngoingJobs(state)

        if (!hasOngoing) {
            autoRefreshJob?.cancel()
            autoRefreshJob = null
            return
        }

        if (autoRefreshJob != null) return

        autoRefreshJob = viewModelScope.launch {
            while (hasOngoingJobs()) {
                kotlinx.coroutines.delay(1000L)

                try {

                    loadJobs()
                    loadExams(forceRefresh = true)
                } catch (_: Exception) {
                }
            }
            autoRefreshJob = null
        }
    }



    private fun bearer() = "Bearer $token"

    private fun loadSubject() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingSubject = true)
            try {
                val response: SubjectResponse =
                    apiService.getSubjectDetail(bearer(), subjectId)
                val subject = response.subject
                _uiState.value = _uiState.value.copy(
                    loadingSubject = false,
                    subjectName = subject.name,
                    subjectDescription = subject.description,
                    groupName = subject.groupId,
                    colorHex = subject.color
                )
                ensureAutoRefresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingSubject = false)
            }
        }
    }

    fun loadPdfs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingPdfs = true)
            try {
                val response: PdfListResponse =
                    apiService.getPdfsBySubject(bearer(), subjectId)
                _uiState.value = _uiState.value.copy(
                    pdfs = response.pdfs,
                    loadingPdfs = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingPdfs = false)
            }
        }
    }

    fun loadJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingJobs = true)
            try {
                val examJobsResponse: JobListResponse =
                    apiService.getExamJobs(bearer(), subjectId)
                val gradingJobsResponse: JobListResponse =
                    apiService.getGradingJobs(bearer(), subjectId)
                _uiState.value = _uiState.value.copy(
                    examJobs = examJobsResponse.jobs,
                    gradingJobs = gradingJobsResponse.jobs,
                    loadingJobs = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingJobs = false)
            }
        }
    }

    fun loadExams(forceRefresh: Boolean = false) {
        if (_uiState.value.loadingExams && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingExams = true, examsError = null)
            try {
                val examResponse: ExamListResponse =
                    apiService.getExamsBySubject(bearer(), subjectId)
                val examJobsResponse: JobListResponse =
                    apiService.getExamJobs(bearer(), subjectId)
                val gradingJobsResponse: JobListResponse =
                    apiService.getGradingJobs(bearer(), subjectId)

                val examJobs = examJobsResponse.jobs
                val gradingJobs = gradingJobsResponse.jobs

                val latestExamJobByExamId: Map<String, JobData?> =
                    examJobs
                        .sortedByDescending { it.createdAt ?: "" }
                        .groupBy { it.examId ?: "" }
                        .mapValues { (_, jobs) -> jobs.firstOrNull() }

                val latestGradingJobByExamId: Map<String, JobData?> =
                    gradingJobs
                        .sortedByDescending { it.createdAt ?: "" }
                        .groupBy { it.examId ?: "" }
                        .mapValues { (_, jobs) -> jobs.firstOrNull() }

                val items = examResponse.exams.map { exam ->
                    val genJob = latestExamJobByExamId[exam.examId]
                    val gradJob = latestGradingJobByExamId[exam.examId]

                    SubjectExamUi(
                        examId = exam.examId,
                        title = exam.title ?: "",
                        numQuestions = exam.numQuestions ?: 0,
                        language = exam.language,
                        difficulty = exam.difficulty,
                        status = exam.status,
                        createdAt = exam.createdAt,
                        generationJobStatus = genJob?.status,
                        generationProgress = genJob?.progressPercentage,
                        gradingJobStatus = gradJob?.status,
                        gradingProgress = gradJob?.progressPercentage,
                    )
                }

                _uiState.value = _uiState.value.copy(
                    exams = items,
                    loadingExams = false,
                    examJobs = examJobs,
                    gradingJobs = gradingJobs
                )
                ensureAutoRefresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadingExams = false,
                    examsError = e.message ?: "시험 목록을 불러오지 못했습니다."
                )
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && it.moveToFirst()) {
                result = it.getString(nameIndex)
            }
        }
        return result ?: "upload.pdf"
    }

    suspend fun uploadPdfs(uris: List<Uri>): Result<Unit> {
        if (uris.isEmpty()) return Result.success(Unit)

        _uiState.value = _uiState.value.copy(
            uploading = true,
            uploadProgress = 0 to uris.size
        )

        return try {
            var index = 0

            for (uri in uris) {
                val inputStream = contentResolver.openInputStream(uri) ?: continue
                val bytes = inputStream.readBytes()
                inputStream.close()

                val fileName = getFileName(uri)

                val mime = contentResolver.getType(uri) ?: "application/pdf"
                val mediaType = try {
                    mime.toMediaType()
                } catch (e: IllegalArgumentException) {
                    "application/pdf".toMediaType()
                }

                val requestBody = bytes.toRequestBody(mediaType)

                val part = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = fileName,
                    body = requestBody
                )

                apiService.uploadPdf(bearer(), subjectId, part)

                index += 1
                _uiState.value = _uiState.value.copy(
                    uploadProgress = index to uris.size
                )
            }

            _uiState.value = _uiState.value.copy(
                uploading = false,
                uploadProgress = null
            )
            loadPdfs()
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                uploading = false,
                uploadProgress = null
            )
            Result.failure(e)
        }
    }

    suspend fun deletePdf(fileId: String): Result<PdfDeleteResponse> {
        _uiState.value = _uiState.value.copy(deletingFileId = fileId)

        return try {
            val response: PdfDeleteResponse =
                apiService.deletePdf(bearer(), subjectId, fileId)
            loadPdfs()

            _uiState.value = _uiState.value.copy(deletingFileId = null)
            Result.success(response)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(deletingFileId = null)
            Result.failure(e)
        }
    }

    suspend fun deleteExam(examId: String): Result<Unit> {
        _uiState.value = _uiState.value.copy(deletingExamId = examId)
        return try {
            val res = apiService.deleteExam(bearer(), subjectId, examId)
            if (res.isSuccessful) {
                loadExams(forceRefresh = true)
                loadJobs()
                _uiState.value = _uiState.value.copy(deletingExamId = null)
                Result.success(Unit)
            } else {
                _uiState.value = _uiState.value.copy(deletingExamId = null)
                Result.failure(
                    Exception("HTTP ${res.code()} ${res.message()}")
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(deletingExamId = null)
            Result.failure(e)
        }
    }
}

class SubjectDetailViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubjectDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubjectDetailViewModel(apiService, token, subjectId, contentResolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// -----------------------------
// COMPOSABLE
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    activity: Activity,
    viewModel: SubjectDetailViewModel = viewModel(
        factory = SubjectDetailViewModelFactory(apiService, token, subjectId, activity.contentResolver)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDeletePdfDialog by remember { mutableStateOf(false) }
    var pdfToDelete by remember { mutableStateOf<PdfData?>(null) }

    var examToDelete by remember { mutableStateOf<SubjectExamUi?>(null) }
    var selectedTab by remember { mutableStateOf(SubjectDetailTab.EXAMS) }

    val brandPrimary = Color(0xFF5BA27F)
    val brandSecondaryText = Color(0xFF4C6070)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    LaunchedEffect(uiState.examsError) {
        uiState.examsError?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            data?.data?.let { uris.add(it) }

            val clipData = data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
            }

            if (uris.isNotEmpty()) {
                scope.launch {
                    val uploadResult = viewModel.uploadPdfs(uris)
                    if (uploadResult.isSuccess) {
                        snackbarHostState.showSnackbar(context.getString(R.string.pdf_upload_success))
                    } else {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.pdf_upload_fail, uploadResult.exceptionOrNull()?.message ?: "")
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.subjectName.ifBlank { stringResource(R.string.subject_detail_title) },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
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

            if (uiState.loadingSubject && uiState.subjectName.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 96.dp
                    )
                ) {
                    item {
                        SubjectHeaderSection(
                            name = uiState.subjectName,
                            description = uiState.subjectDescription,
                            groupName = uiState.groupName,
                            colorHex = uiState.colorHex
                        )
                    }

                    // 시험 | PDF 탭
                    item {
                        SubjectTabRow(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    // 탭에 따라 분기
                    if (selectedTab == SubjectDetailTab.EXAMS) {
                        item {
                            JobProgressBanner(
                                examJobs = uiState.examJobs,
                                exams = uiState.exams,
                                gradingJobs = uiState.gradingJobs
                            )
                        }
                        item {
                            ExamListSection(
                                uiState = uiState,
                                onRefresh = { viewModel.loadExams(forceRefresh = true) },
                                onTakeExam = { exam ->
                                    navController.navigate(
                                        Screen.TakeExam.route(
                                            subjectId,
                                            exam.examId
                                        )
                                    )
                                },
                                onViewResult = { exam ->
                                    navController.navigate(
                                        Screen.ExamDetail.route(
                                            subjectId,
                                            exam.examId
                                        )
                                    )
                                },
                                onDeleteExam = { exam ->
                                    examToDelete = exam
                                }
                            )
                        }
                    } else {
                        // PDF 탭
                        item {
                            JobProgressBanner(
                                examJobs = uiState.examJobs,
                                exams = uiState.exams,
                                gradingJobs = uiState.gradingJobs
                            )
                        }
                        item {
                            PdfSectionHeader(
                                onUploadClick = {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            putExtra(
                                                DocumentsContract.EXTRA_INITIAL_URI,
                                                MediaStore.Downloads.EXTERNAL_CONTENT_URI
                                            )
                                        }
                                    }
                                    filePickerLauncher.launch(intent)
                                },
                                uploading = uiState.uploading,
                                uploadProgress = uiState.uploadProgress
                            )
                        }


                        // PDF 리스트
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.96f)
                                ),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    when {
                                        uiState.loadingPdfs && uiState.pdfs.isEmpty() -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }

                                        uiState.pdfs.isEmpty() -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(stringResource(R.string.pdf_empty))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    stringResource(R.string.pdf_empty_guide),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = brandSecondaryText
                                                )
                                            }
                                        }

                                        else -> {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                uiState.pdfs.forEach { pdf ->
                                                    val isDeleting =
                                                        uiState.deletingFileId == pdf.fileId
                                                    PdfRow(
                                                        pdf = pdf,
                                                        onOpen = {
                                                            navController.navigate("subjects/$subjectId/pdfs/${pdf.fileId}")
                                                        },
                                                        onDelete = {
                                                            pdfToDelete = pdf
                                                            showDeletePdfDialog = true
                                                        },
                                                        onGenerateExam = {
                                                            navController.navigate("subjects/$subjectId/generate-exam")
                                                        },
                                                        isDeleting = isDeleting
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // 하단 고정 "시험 생성" 버튼 (대각선 그라데이션 애니메이션)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // 파랑 + 청록 + 초록 살짝 섞은 그라데이션
                val gradientColors = listOf(
                    Color(0xFF3BA9FF), // 파랑
                    Color(0xFF32D6C4), // 청록
                    Color(0xFF22C55E)  // 초록
                )

                val infiniteTransition = rememberInfiniteTransition(label = "exam-button-gradient-diagonal")

                // 대각선 방향으로 흘러가는 offset
                val animatedOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 3500,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "exam-button-offset-diagonal"
                )

                val brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(
                        x = -400f * animatedOffset,
                        y = -200f * animatedOffset
                    ),
                    end = Offset(
                        x = 400f * animatedOffset,
                        y = 200f * animatedOffset
                    )
                )

                Button(
                    onClick = {
                        navController.navigate("subjects/$subjectId/generate-exam")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            brush = brush,
                            shape = RoundedCornerShape(999.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_generate_exam),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // PDF 삭제 다이얼로그
            if (showDeletePdfDialog && pdfToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeletePdfDialog = false
                        pdfToDelete = null
                    },
                    title = { Text(stringResource(R.string.pdf_delete_title)) },
                    text = {
                        Text(
                            stringResource(R.string.pdf_delete_confirm, pdfToDelete?.originalFilename ?: "")
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val target = pdfToDelete
                                if (target != null) {
                                    scope.launch {
                                        val result = viewModel.deletePdf(target.fileId)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar(context.getString(R.string.pdf_delete_success))
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.pdf_delete_fail, result.exceptionOrNull()?.message ?: "")
                                            )
                                        }
                                        showDeletePdfDialog = false
                                        pdfToDelete = null
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_delete))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                showDeletePdfDialog = false
                                pdfToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            // 시험 삭제 다이얼로그
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
                                                context.getString(R.string.exam_delete_fail)
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

// -----------------------------
// 하위 Composable 들
// -----------------------------
@Composable
private fun SubjectHeaderSection(
    name: String,
    description: String?,
    groupName: String?,
    colorHex: String?
) {
    val color = try {
        if (colorHex.isNullOrBlank()) null
        else Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!groupName.isNullOrBlank()) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (color != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .fillMaxWidth(0.15f)
                            .background(color)
                    )
                    Text(
                        text = stringResource(R.string.subject_color_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// 시험 | PDF 탭 스위치
@Composable
private fun SubjectTabRow(
    selectedTab: SubjectDetailTab,
    onTabSelected: (SubjectDetailTab) -> Unit
) {
    val brandPrimary = Color(0xFF5BA27F)
    val inactiveText = Color(0xFF4C6070)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onTabSelected(SubjectDetailTab.EXAMS) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedTab == SubjectDetailTab.EXAMS)
                    brandPrimary.copy(alpha = 0.12f) else Color.Transparent,
                contentColor = if (selectedTab == SubjectDetailTab.EXAMS)
                    brandPrimary else inactiveText
            )
        ) {
            Text(stringResource(R.string.tab_exams))
        }
        OutlinedButton(
            onClick = { onTabSelected(SubjectDetailTab.PDF) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedTab == SubjectDetailTab.PDF)
                    brandPrimary.copy(alpha = 0.12f) else Color.Transparent,
                contentColor = if (selectedTab == SubjectDetailTab.PDF)
                    brandPrimary else inactiveText
            )
        ) {
            Text(stringResource(R.string.tab_pdf))
        }
    }
}

@Composable
private fun PdfSectionHeader(
    onUploadClick: () -> Unit,
    uploading: Boolean,
    uploadProgress: Pair<Int, Int>?
) {
    val brandPrimary = Color(0xFF5BA27F)
    val brandSecondaryText = Color(0xFF4C6070)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.pdf_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(R.string.pdf_section_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = brandSecondaryText
                )
            }
            Button(
                onClick = onUploadClick,
                enabled = !uploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (uploading) stringResource(R.string.action_uploading) else stringResource(R.string.action_upload_pdf))
            }
        }
        if (uploading && uploadProgress != null) {
            val (current, total) = uploadProgress
            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                LinearProgressIndicator(progress = progress)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pdf_upload_progress_fmt, current, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = brandSecondaryText
                )
            }
        }
    }
}

@Composable
private fun PdfRow(
    pdf: PdfData,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onGenerateExam: () -> Unit,
    isDeleting: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isDeleting,
                onClick = onOpen
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "PDF",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = pdf.originalFilename,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = String.format("%.2f MB", pdf.size / 1024f / 1024f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

// PDF 탭 상단: 생성/채점 중인 시험 진행률 카드
@Composable
private fun JobProgressBanner(
    examJobs: List<JobData>,
    exams: List<SubjectExamUi>,
    gradingJobs: List<JobData>,
) {
    val brandSecondaryText = Color(0xFF4C6070)

    val ongoingExamJobs = examJobs.filter {
        it.status == "processing" || it.status == "pending"
    }
    val ongoingGradingJobs = gradingJobs.filter {
        it.status == "processing" || it.status == "pending"
    }

    if (ongoingExamJobs.isEmpty() && ongoingGradingJobs.isEmpty()) return

    val examById = exams.associateBy { it.examId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "생성/채점 중인 시험",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            ongoingExamJobs.forEach { job ->
                val examId = job.examId
                val examFromList = examId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { examById[it] }

                val candidates = listOfNotNull(
                    examFromList?.title?.takeIf { it.isNotBlank() },

                    if (!examId.isNullOrBlank())
                        "시험 #${examId.takeLast(6)}"
                    else null,
                    "새 시험"
                )

                val examTitle = candidates.first()

                val progressDouble = (job.progressPercentage ?: 0.0).coerceIn(0.0, 100.0)
                val progressFloat = (progressDouble / 100.0).toFloat()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "$examTitle · 생성 중",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progressFloat,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${progressDouble.toInt()}% 진행 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = brandSecondaryText
                    )
                }
            }

            ongoingGradingJobs.forEach { job ->
                val examId = job.examId ?: ""
                val rawTitle = examById[examId]?.title
                val examTitle = if (!rawTitle.isNullOrBlank()) {
                    rawTitle
                } else {
                    "시험 #${examId.takeLast(6)}"
                }
                val progressDouble = (job.progressPercentage ?: 0.0).coerceIn(0.0, 100.0)
                val progressFloat = (progressDouble / 100.0).toFloat()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "$examTitle · 채점 중",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progressFloat,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${progressDouble.toInt()}% 진행 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = brandSecondaryText
                    )
                }
            }
        }
    }
}

// 시험 탭 내용
@Composable
private fun ExamListSection(
    uiState: SubjectDetailUiState,
    onRefresh: () -> Unit,
    onTakeExam: (SubjectExamUi) -> Unit,
    onViewResult: (SubjectExamUi) -> Unit,
    onDeleteExam: (SubjectExamUi) -> Unit
) {
    val accentColor = try {
        if (uiState.colorHex.isNullOrBlank()) null
        else Color(android.graphics.Color.parseColor(uiState.colorHex))
    } catch (e: IllegalArgumentException) {
        null
    } ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "시험 목록",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        when {
            uiState.loadingExams && uiState.exams.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.exams.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.96f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "이 과목에는 아직 생성된 시험이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "아래 '시험 생성' 버튼으로 첫 시험을 만들어 보세요.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.exams.forEach { exam ->
                        val isDeleting = uiState.deletingExamId == exam.examId

                            ExamRow(
                                exam = exam,
                                accentColor = accentColor,
                                isDeleting = isDeleting,
                                onPrimary = {
                                    when {
                                        exam.canViewResult -> onViewResult(exam)
                                        exam.canTakeExam -> onTakeExam(exam)
                                        else -> Unit
                                    }
                                },
                                onTakeExam = { onTakeExam(exam) },
                                onViewResult = { onViewResult(exam) },
                                onDelete = { onDeleteExam(exam) },
                                difficultyLabel = when (exam.difficulty) {
                                    "easy" -> stringResource(R.string.difficulty_easy)
                                    "medium" -> stringResource(R.string.difficulty_medium)
                                    "hard" -> stringResource(R.string.difficulty_hard)
                                    else -> exam.difficulty ?: stringResource(R.string.difficulty_unknown)
                                },
                                languageLabel = exam.language ?: stringResource(R.string.language_unspecified),
                                activeJobLabel = when {
                                    exam.generationJobStatus == "processing" || exam.generationJobStatus == "pending" -> stringResource(R.string.status_processing)
                                    exam.gradingJobStatus == "processing" || exam.gradingJobStatus == "pending" -> stringResource(R.string.status_grading)
                                    else -> null
                                }
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamRow(
    exam: SubjectExamUi,
    accentColor: Color,
    isDeleting: Boolean,
    onPrimary: () -> Unit,
    onTakeExam: () -> Unit,
    onViewResult: () -> Unit,
    onDelete: () -> Unit,
    difficultyLabel: String,
    languageLabel: String,
    activeJobLabel: String?
) {
    val brandSecondaryText = Color(0xFF4C6070)

    val isGradingInProgress =
        exam.gradingJobStatus == "processing" || exam.gradingJobStatus == "pending"

    val (primaryLabel, primaryEnabled) = when {
        exam.canViewResult -> stringResource(R.string.action_view_result) to true
        isGradingInProgress -> stringResource(R.string.status_grading) to false
        exam.canTakeExam -> stringResource(R.string.action_take_exam) to true
        else -> stringResource(R.string.status_grading) to false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = primaryEnabled && !isDeleting) { onPrimary() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.exam_questions_fmt, exam.numQuestions),
                    style = MaterialTheme.typography.labelSmall,
                    color = brandSecondaryText
                )
                Text(
                    text = stringResource(R.string.exam_difficulty_fmt, difficultyLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = brandSecondaryText
                )
                Text(
                    text = languageLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = brandSecondaryText
                )
            }

            exam.createdAt?.let { created ->
                Text(
                    text = created,
                    style = MaterialTheme.typography.bodySmall,
                    color = brandSecondaryText
                )
            }

            if (exam.hasOngoingJob && exam.progress != null && activeJobLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))

                val pDouble = exam.progress!!.coerceIn(0.0, 100.0)
                val pFloat = (pDouble / 100.0).toFloat()

                LinearProgressIndicator(
                    progress = pFloat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.job_progress_fmt, pDouble.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = brandSecondaryText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }

                OutlinedButton(
                    onClick = {
                        when {
                            exam.canViewResult -> onViewResult()
                            exam.canTakeExam -> onTakeExam()
                            else -> Unit
                        }
                    },
                    enabled = primaryEnabled && !isDeleting
                ) {
                    Text(primaryLabel)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    enabled = !isDeleting
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete)
                    )
                }
            }
        }
    }
}
