package com.example.testme.ui.screens.exam

import android.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.JobResponse
import com.example.testme.data.model.LanguageListResponse
import com.example.testme.data.model.PdfData
import com.example.testme.data.model.PdfListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Difficulty(val value: String, val label: String) {
    EASY("easy", "쉬움"),
    MEDIUM("medium", "보통"),
    HARD("hard", "어려움")
}

data class LanguageUi(
    val code: String,
    val name: String,
    val nativeName: String
)

data class GenerateExamUiState(
    val pdfs: List<PdfData> = emptyList(),
    val loadingPdfs: Boolean = false,
    val selectedPdfIds: Set<String> = emptySet(),
    val numQuestions: Int = 10,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val languages: List<LanguageUi> = emptyList(),
    val loadingLanguages: Boolean = false,
    val languageCode: String = "ko",
    val submitting: Boolean = false
)

class GenerateExamViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateExamUiState())
    val uiState: StateFlow<GenerateExamUiState> = _uiState

    fun loadPdfs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingPdfs = true)
            try {
                val res: PdfListResponse =
                    apiService.getPdfsBySubject("Bearer $token", subjectId)
                _uiState.value = _uiState.value.copy(
                    pdfs = res.pdfs,
                    loadingPdfs = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingPdfs = false)
            }
        }
    }

    fun loadLanguages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingLanguages = true)
            try {
                val res: LanguageListResponse = apiService.getSupportedLanguages()
                val langs = res.languages.map {
                    LanguageUi(
                        code = it.code,
                        name = it.name,
                        nativeName = it.nativeName
                    )
                }
                val defaultCode = _uiState.value.languageCode.takeIf { code ->
                    langs.any { it.code == code }
                } ?: langs.firstOrNull()?.code ?: "ko"

                _uiState.value = _uiState.value.copy(
                    languages = langs,
                    languageCode = defaultCode,
                    loadingLanguages = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingLanguages = false)
            }
        }
    }

    fun togglePdf(fileId: String) {
        val current = _uiState.value
        val set = current.selectedPdfIds.toMutableSet()
        if (set.contains(fileId)) set.remove(fileId) else set.add(fileId)
        _uiState.value = current.copy(selectedPdfIds = set)
    }

    fun allSelected(): Boolean {
        val state = _uiState.value
        return state.pdfs.isNotEmpty() && state.pdfs.size == state.selectedPdfIds.size
    }

    fun toggleSelectAll() {
        val state = _uiState.value
        _uiState.value = if (allSelected()) {
            state.copy(selectedPdfIds = emptySet())
        } else {
            state.copy(selectedPdfIds = state.pdfs.map { it.fileId }.toSet())
        }
    }

    fun updateNumQuestions(n: Int) {
        _uiState.value = _uiState.value.copy(numQuestions = n)
    }

    fun updateDifficulty(difficulty: Difficulty) {
        _uiState.value = _uiState.value.copy(difficulty = difficulty)
    }

    fun updateLanguage(code: String) {
        _uiState.value = _uiState.value.copy(languageCode = code)
    }

    suspend fun submit(): Result<JobResponse> {
        val state = _uiState.value
        if (state.selectedPdfIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("최소 1개의 PDF를 선택해주세요."))
        }
        _uiState.value = state.copy(submitting = true)
        return try {
            val body = mapOf(
                "num_questions" to state.numQuestions,
                "difficulty" to state.difficulty.value,
                "ai_provider" to "openai",
                "ai_model" to "gpt-4.1-mini",
                "language" to state.languageCode,
                "pdf_ids" to state.selectedPdfIds.toList()
            )
            val res = apiService.generateExam(
                token = "Bearer $token",
                subjectId = subjectId,
                body = body
            )
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.success(res)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.failure(e)
        }
    }
}

class GenerateExamViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenerateExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GenerateExamViewModel(
                apiService = apiService,
                token = token,
                subjectId = subjectId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateExamScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    viewModel: GenerateExamViewModel = viewModel(
        factory = GenerateExamViewModelFactory(apiService, token, subjectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadPdfs()
        viewModel.loadLanguages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시험 생성") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding ->
        if (uiState.loadingPdfs && uiState.pdfs.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "선택한 PDF를 기반으로 AI가 자동으로 시험을 생성합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PDF 선택",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedButton(
                                onClick = { viewModel.toggleSelectAll() },
                                enabled = uiState.pdfs.isNotEmpty() && !uiState.submitting
                            ) {
                                Text(
                                    text = if (viewModel.allSelected()) "전체 해제" else "전체 선택"
                                )
                            }
                        }

                        if (uiState.pdfs.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "업로드된 PDF가 없습니다. 과목 화면에서 먼저 PDF를 업로드해 주세요.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(
                                        com.example.testme.ui.navigation.Screen.SubjectDetail.route(subjectId)
                                    )
                                }
                            ) {
                                Text("과목 화면으로 이동")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.pdfs) { pdf ->
                                    PdfSelectableItem(
                                        pdf = pdf,
                                        selected = uiState.selectedPdfIds.contains(pdf.fileId),
                                        enabled = !uiState.submitting,
                                        onClick = { viewModel.togglePdf(pdf.fileId) }
                                    )
                                }
                            }
                            Text(
                                "선택된 PDF: ${uiState.selectedPdfIds.size}개",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("출제 옵션", style = MaterialTheme.typography.titleMedium)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("문제 수", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 10, 20).forEach { n ->
                                    OutlinedButton(
                                        onClick = { viewModel.updateNumQuestions(n) },
                                        enabled = !uiState.submitting,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("${n}문제")
                                    }
                                }
                            }
                            Text(
                                "현재: ${uiState.numQuestions}문제",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("난이도", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Difficulty.values().forEach { diff ->
                                    OutlinedButton(
                                        onClick = { viewModel.updateDifficulty(diff) },
                                        enabled = !uiState.submitting,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(diff.label)
                                    }
                                }
                            }
                            Text(
                                "현재: ${uiState.difficulty.label}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("출제 언어", style = MaterialTheme.typography.labelMedium)
                            when {
                                uiState.loadingLanguages -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            "언어 목록 로딩 중...",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                uiState.languages.isEmpty() -> {
                                    Text(
                                        "사용 가능한 언어 정보를 불러오지 못했습니다. 기본값: ${uiState.languageCode}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        uiState.languages.forEach { lang ->
                                            OutlinedButton(
                                                onClick = { viewModel.updateLanguage(lang.code) },
                                                enabled = !uiState.submitting,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(lang.nativeName)
                                            }
                                        }
                                    }
                                    Text(
                                        "선택된 언어: ${
                                            uiState.languages.firstOrNull { it.code == uiState.languageCode }?.nativeName
                                                ?: uiState.languageCode
                                        }",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val result = viewModel.submit()
                            if (result.isSuccess) {
                                snackBarHostState.showSnackbar("시험 생성이 시작되었습니다.")
                                navController.popBackStack()
                            } else {
                                snackBarHostState.showSnackbar(
                                    result.exceptionOrNull()?.message
                                        ?: "시험 생성에 실패했습니다."
                                )
                            }
                        }
                    },
                    enabled = uiState.selectedPdfIds.isNotEmpty() && !uiState.submitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (uiState.submitting) "생성 중..." else "시험 생성")
                }
            }
        }
    }
}

@Composable
private fun PdfSelectableItem(
    pdf: PdfData,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pdf.originalFilename, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = String.format("%.2f MB", pdf.size / 1024f / 1024f),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (selected) "✓" else "",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}