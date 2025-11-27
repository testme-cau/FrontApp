package com.example.testme.ui.screens.subject

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.testme.data.model.JobData
import com.example.testme.data.model.JobListResponse
import com.example.testme.data.model.PdfData
import com.example.testme.data.model.PdfDeleteResponse
import com.example.testme.data.model.PdfListResponse
import com.example.testme.data.model.SubjectData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.graphics.Color
import com.example.testme.data.model.SubjectResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pdfToDelete by remember { mutableStateOf<PdfData?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (!uris.isNullOrEmpty()) {
                scope.launch {
                    val result = viewModel.uploadPdfs(uris)
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("PDF 업로드 완료")
                    } else {
                        snackbarHostState.showSnackbar(
                            result.exceptionOrNull()?.message ?: "PDF 업로드 실패"
                        )
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.subjectName.ifBlank { "과목 상세" }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("subjects/$subjectId/exams")
                        }
                    ) {
                        Text("시험 목록")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (uiState.loadingSubject && uiState.subjectName.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SubjectHeaderSection(
                        name = uiState.subjectName,
                        description = uiState.subjectDescription,
                        groupName = uiState.groupName,
                        colorHex = uiState.colorHex
                    )
                }

                item {
                    PdfSectionHeader(
                        onUploadClick = {
                            filePickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        uploading = uiState.uploading,
                        uploadProgress = uiState.uploadProgress
                    )
                }

                item {
                    if (uiState.loadingPdfs && uiState.pdfs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.pdfs.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("업로드된 PDF가 없습니다.")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "오른쪽 상단 버튼으로 PDF를 업로드해 주세요.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.pdfs.forEach { pdf ->
                                PdfRow(
                                    pdf = pdf,
                                    onOpen = {
                                        navController.navigate("subjects/$subjectId/pdfs/${pdf.fileId}")
                                    },
                                    onDelete = {
                                        pdfToDelete = pdf
                                        showDeleteDialog = true
                                    },
                                    onGenerateExam = {
                                        navController.navigate("subjects/$subjectId/generate-exam")
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    JobSummarySection(
                        examJobs = uiState.examJobs,
                        gradingJobs = uiState.gradingJobs,
                        loadingJobs = uiState.loadingJobs,
                        onRefresh = { viewModel.loadJobs() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        if (showDeleteDialog && pdfToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("PDF 삭제") },
                text = {
                    Text(
                        "\"${pdfToDelete?.originalFilename}\" 파일을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
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
                                        snackbarHostState.showSnackbar("삭제 완료")
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            result.exceptionOrNull()?.message ?: "삭제 실패"
                                        )
                                    }
                                    showDeleteDialog = false
                                    pdfToDelete = null
                                }
                            }
                        }
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            pdfToDelete = null
                        }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        if (groupName != null) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (color != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {
                Text(
                    text = "이 색상은 과목 카드와 관련 UI에 사용됩니다.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PdfSectionHeader(
    onUploadClick: () -> Unit,
    uploading: Boolean,
    uploadProgress: Pair<Int, Int>?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PDF 파일",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "업로드된 교재 PDF를 기반으로 시험을 생성합니다.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = onUploadClick,
            enabled = !uploading
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Upload"
            )
            Spacer(modifier = Modifier.height(0.dp))
            Text(text = if (uploading) "업로드 중..." else "PDF 업로드")
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
                text = "$current / $total 파일 업로드 중",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PdfRow(
    pdf: PdfData,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onGenerateExam: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "PDF",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = pdf.originalFilename,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${pdf.size ?: 0} 페이지 • ${(pdf.size / 1024f / 1024f).let { String.format("%.2f MB", it) }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onGenerateExam) {
                    Text("시험 생성")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun JobSummarySection(
    examJobs: List<JobData>,
    gradingJobs: List<JobData>,
    loadingJobs: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Text(
                    text = "작업 현황",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !loadingJobs
                ) {
                    Text("새로고침")
                }
            }
            if (loadingJobs && examJobs.isEmpty() && gradingJobs.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "작업 목록을 불러오는 중...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "시험 생성 작업",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "진행 중 ${examJobs.count { it.status == "processing" || it.status == "pending" }}건",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column {
                        Text(
                            text = "채점 작업",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "진행 중 ${gradingJobs.count { it.status == "processing" || it.status == "pending" }}건",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

data class SubjectDetailUiState(
    val loadingSubject: Boolean = false,
    val subjectName: String = "",
    val subjectDescription: String? = null,
    val groupName: String? = null,
    val colorHex: String? = null,
    val loadingPdfs: Boolean = false,
    val pdfs: List<PdfData> = emptyList(),
    val uploading: Boolean = false,
    val uploadProgress: Pair<Int, Int>? = null,
    val loadingJobs: Boolean = false,
    val examJobs: List<JobData> = emptyList(),
    val gradingJobs: List<JobData> = emptyList()
)

class SubjectDetailViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState

    fun loadAll() {
        loadSubject()
        loadPdfs()
        loadJobs()
    }

    private fun loadSubject() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingSubject = true)
            try {
                val response: SubjectResponse =
                    apiService.getSubjectDetail("Bearer $token", subjectId)
                val subject = response.subject
                _uiState.value = _uiState.value.copy(
                    loadingSubject = false,
                    subjectName = subject.name,
                    subjectDescription = subject.description,
                    groupName = subject.groupId,
                    colorHex = subject.color
                )
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
                    apiService.getPdfsBySubject("Bearer $token", subjectId)
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
                    apiService.getExamJobs("Bearer $token", subjectId)
                val gradingJobsResponse: JobListResponse =
                    apiService.getGradingJobs("Bearer $token", subjectId)
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

                val fileName = uri.lastPathSegment ?: "upload.pdf"

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

                apiService.uploadPdf("Bearer $token", subjectId, part)

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
        return try {
            val response: PdfDeleteResponse =
                apiService.deletePdf("Bearer $token", subjectId, fileId)
            loadPdfs()
            Result.success(response)
        } catch (e: Exception) {
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
