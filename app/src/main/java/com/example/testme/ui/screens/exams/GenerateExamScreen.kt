package com.example.testme.ui.screens.exam

import retrofit2.HttpException
import android.util.Log
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.testme.R
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.GenerateExamRequest
import com.example.testme.data.model.JobResponse
import com.example.testme.data.model.LanguageListResponse
import com.example.testme.data.model.PdfData
import com.example.testme.data.model.PdfListResponse
import com.example.testme.ui.navigation.Screen
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Difficulty(val value: String, val label: String) {
    EASY("easy", "easy"), // Actual label handled in UI
    MEDIUM("medium", "medium"),
    HARD("hard", "hard")
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

    suspend fun submit(context: android.content.Context): Result<JobResponse> {
        val state = _uiState.value
        if (state.selectedPdfIds.isEmpty()) {
            return Result.failure(IllegalArgumentException(context.getString(R.string.min_pdf_error)))
        }

        _uiState.value = state.copy(submitting = true)
        return try {
            val body = GenerateExamRequest(
                numQuestions = state.numQuestions,
                difficulty = state.difficulty.value,
                aiProvider = "gpt",
                aiModel = "gpt-4.1-mini",
                language = state.languageCode,
                pdfIds = state.selectedPdfIds.toList()
            )

            val res = apiService.generateExam(
                token = "Bearer $token",
                subjectId = subjectId,
                body = body
            )
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.success(res)
        } catch (e: Exception) {
            if (e is HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("GenerateExam", "HTTP error $code, body = $errorBody")
            } else {
                Log.e("GenerateExam", "Unknown error", e)
            }

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
    subjectColor: Color = MaterialTheme.colorScheme.primary,
    viewModel: GenerateExamViewModel = viewModel(
        factory = GenerateExamViewModelFactory(apiService, token, subjectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadPdfs()
        viewModel.loadLanguages()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.generate_exam_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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

            if (uiState.loadingPdfs && uiState.pdfs.isEmpty()) {
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
                                    text = stringResource(R.string.loading_pdfs_title),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.loading_pdfs_desc),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.generate_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            subjectColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            subjectColor.copy(alpha = 0.16f),
                                            Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.pdf_select_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.toggleSelectAll() },
                                        enabled = uiState.pdfs.isNotEmpty() && !uiState.submitting
                                    ) {
                                        Text(
                                            text = if (viewModel.allSelected()) stringResource(R.string.pdf_deselect_all) else stringResource(R.string.pdf_select_all)
                                        )
                                    }
                                }

                                if (uiState.pdfs.isEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.generate_empty_pdfs),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            navController.navigate(
                                                Screen.SubjectDetail.route(subjectId)
                                            )
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_go_to_subject))
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
                                                onClick = { viewModel.togglePdf(pdf.fileId) },
                                                accentColor = subjectColor
                                            )
                                        }
                                    }
                                    Text(
                                        stringResource(R.string.selected_pdfs_count_fmt, uiState.selectedPdfIds.size),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            subjectColor.copy(alpha = 0.18f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            subjectColor.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.option_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.option_num_questions), style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(5, 10, 20).forEach { n ->
                                            val isSelected = uiState.numQuestions == n
                                            val buttonModifier = Modifier.weight(1f)

                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.updateNumQuestions(n) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier
                                                ) {
                                                    Text("${n}" + stringResource(R.string.questions_suffix))
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateNumQuestions(n) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier
                                                ) {
                                                    Text("${n}" + stringResource(R.string.questions_suffix))
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        stringResource(R.string.current_option_fmt, "${uiState.numQuestions}" + stringResource(R.string.questions_suffix)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.option_difficulty), style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Difficulty.values().forEach { diff ->
                                            val isSelected = uiState.difficulty == diff
                                            val buttonModifier = Modifier.weight(1f)
                                            val label = when (diff) {
                                                Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                                                Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                                                Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                                            }

                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.updateDifficulty(diff) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier
                                                ) {
                                                    Text(label)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateDifficulty(diff) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier
                                                ) {
                                                    Text(label)
                                                }
                                            }
                                        }
                                    }
                                    val currentDiffLabel = when (uiState.difficulty) {
                                        Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                                        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                                        Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                                    }
                                    Text(
                                        stringResource(R.string.current_option_fmt, currentDiffLabel),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                LanguageSelector(
                                    languages = uiState.languages,
                                    selectedCode = uiState.languageCode,
                                    loading = uiState.loadingLanguages,
                                    enabled = !uiState.submitting,
                                    onSelect = { viewModel.updateLanguage(it) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val result = viewModel.submit(context)
                                if (result.isSuccess) {
                                    navController.popBackStack()
                                    launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.generate_start_success))
                                    }
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.exceptionOrNull()?.message
                                            ?: context.getString(R.string.generate_fail)
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
                        Text(if (uiState.submitting) stringResource(R.string.action_generating) else stringResource(R.string.action_generate_exam))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    languages: List<LanguageUi>,
    selectedCode: String,
    loading: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLang = languages.firstOrNull { it.code == selectedCode }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.option_language), style = MaterialTheme.typography.labelMedium)

        when {
            loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        stringResource(R.string.language_loading),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            languages.isEmpty() -> {
                Text(
                    stringResource(R.string.language_load_fail, selectedCode),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            else -> {
                Box {
                    OutlinedButton(
                        onClick = { if (enabled) expanded = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedLang?.nativeName ?: selectedCode.uppercase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.label_language_code_fmt, lang.nativeName, lang.code.uppercase()))
                                },
                                onClick = {
                                    onSelect(lang.code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.selected_language_fmt, (selectedLang?.nativeName ?: selectedCode.uppercase())),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PdfSelectableItem(
    pdf: PdfData,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 4.dp else 1.dp,
        color = if (selected)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(
                1.dp,
                accentColor.copy(alpha = 0.5f)
            )
        else
            null
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
            }
            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = accentColor
                    )
                )
            }
        }
    }
}