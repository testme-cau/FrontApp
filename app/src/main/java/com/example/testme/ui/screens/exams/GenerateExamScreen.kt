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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val numQuestions: Int = 5,
    val difficulty: Difficulty = Difficulty.EASY,
    val languages: List<LanguageUi> = emptyList(),
    val loadingLanguages: Boolean = false,
    val languageCode: String = "en",
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
                val langs = res.languages
                    .filter { it.code == "ko" || it.code == "en" } // Limit to KO and EN
                    .map {
                    LanguageUi(
                        code = it.code,
                        name = it.name,
                        nativeName = it.nativeName
                    )
                }
                val defaultCode = _uiState.value.languageCode.takeIf { code ->
                    langs.any { it.code == code }
                } ?: langs.firstOrNull()?.code ?: "en"

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
                                modifier = Modifier.size(24.dp),
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
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.generate_desc),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    lineHeight = 20.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 0.dp,
                        shadowElevation = 6.dp,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            subjectColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing
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
                                            navController.popBackStack()
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_go_to_subject))
                                    }
                                } else {
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
                        tonalElevation = 0.dp,
                        shadowElevation = 6.dp,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            subjectColor.copy(alpha = 0.18f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                            val color = when (n) {
                                                5 -> Color(0xFF4CAF50) // Green
                                                10 -> Color(0xFF2196F3) // Blue
                                                20 -> Color(0xFFF44336) // Red
                                                else -> MaterialTheme.colorScheme.primary
                                            }

                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.updateNumQuestions(n) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = color,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Text("${n}" + stringResource(R.string.questions_suffix))
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateNumQuestions(n) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier,
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = Color.Black
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                                ) {
                                                    Text("${n}" + stringResource(R.string.questions_suffix))
                                                }
                                            }
                                        }
                                    }
                                    // Current label removed
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

                                            val color = when (diff) {
                                                Difficulty.EASY -> Color(0xFF4CAF50)
                                                Difficulty.MEDIUM -> Color(0xFF2196F3)
                                                Difficulty.HARD -> Color(0xFFF44336)
                                            }

                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.updateDifficulty(diff) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = color,
                                                        contentColor = Color.White
                                                    ),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                                                    )
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateDifficulty(diff) },
                                                    enabled = !uiState.submitting,
                                                    modifier = buttonModifier,
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = Color.Black
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Current label removed
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
                            if (!uiState.submitting) {
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
                            }
                        },
                        enabled = uiState.selectedPdfIds.isNotEmpty() || uiState.submitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        } else {
                            Text(stringResource(R.string.action_generate_exam))
                        }
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
    val pastelMint = Color(0xFFE8F5E9) // Soft Mint Pastel Color

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.option_language), style = MaterialTheme.typography.labelMedium)

        when {
            loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
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
                    val emoji = when(selectedCode) {
                        "ko" -> "🇰🇷"
                        "en" -> "🇺🇸"
                        else -> "🌐"
                    }
                    
                    Button(
                        onClick = { if (enabled) expanded = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pastelMint,
                            contentColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "$emoji  ${selectedLang?.nativeName ?: selectedCode.uppercase()}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { lang ->
                            val langEmoji = when(lang.code) {
                                "ko" -> "🇰🇷"
                                "en" -> "🇺🇸"
                                else -> "🌐"
                            }
                            DropdownMenuItem(
                                text = {
                                    Text("$langEmoji  ${lang.nativeName} (${lang.code.uppercase()})")
                                },
                                onClick = {
                                    onSelect(lang.code)
                                    expanded = false
                                }
                            )
                        }
                    }
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
    onClick: () -> Unit,
    accentColor: Color
) {
    // Colors based on selection state
    val containerColor = if (selected) Color(0xFFE3F2FD) else Color.White
    val borderColor = if (selected) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.5f)
    val iconColor = if (selected) Color(0xFF2196F3) else Color.LightGray

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 4.dp else 1.dp,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            borderColor
        )
    ) {
        // Box wrapper for Ripple Clip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.originalFilename,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Unselected",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}