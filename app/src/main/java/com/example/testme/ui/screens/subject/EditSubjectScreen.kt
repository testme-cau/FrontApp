package com.example.testme.ui.screens.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.SubjectData
import com.example.testme.data.model.SubjectResponse
import com.example.testme.data.model.SubjectUpdateRequest
import com.example.testme.data.model.group.GroupData
import com.example.testme.data.model.group.GroupListResponse
import com.example.testme.ui.screens.home.SoftBlobBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditSubjectUiState(
    val name: String = "",
    val description: String = "",
    val selectedGroupId: String? = null,
    val color: Color = Color(0xFFEF4444),
    val availableColors: List<Color> = listOf(
        Color(0xFFEF4444),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF6B7280)
    ),
    val groups: List<GroupData> = emptyList(),
    val loading: Boolean = false,
    val loadingGroups: Boolean = false,
    val submitting: Boolean = false
)

class EditSubjectViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSubjectUiState())
    val uiState: StateFlow<EditSubjectUiState> = _uiState

    init {
        loadSubject()
        loadGroups()
    }

    private fun parseColor(value: String?): Color {
        if (value.isNullOrBlank()) return Color(0xFFEF4444)
        return try {
            Color(android.graphics.Color.parseColor(value))
        } catch (e: IllegalArgumentException) {
            Color(0xFFEF4444)
        }
    }

    private fun loadSubject() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val response: SubjectResponse =
                    apiService.getSubjectDetail("Bearer $token", subjectId)
                val subject: SubjectData = response.subject
                _uiState.value = _uiState.value.copy(
                    name = subject.name,
                    description = subject.description ?: "",
                    selectedGroupId = subject.groupId,
                    color = parseColor(subject.color),
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingGroups = true)
            try {
                val response: GroupListResponse =
                    apiService.getGroups("Bearer $token")
                _uiState.value = _uiState.value.copy(
                    groups = response.groups,
                    loadingGroups = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingGroups = false)
            }
        }
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun updateDescription(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun updateGroup(groupId: String?) {
        _uiState.value = _uiState.value.copy(selectedGroupId = groupId)
    }

    fun updateColor(color: Color) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    suspend fun submit(): Result<Unit> {
        val state = _uiState.value
        if (state.name.isBlank()) {
            return Result.failure(IllegalArgumentException("과목명을 입력해주세요."))
        }
        _uiState.value = state.copy(submitting = true)
        return try {
            val argb = state.color.toArgb()
            val rgb = argb and 0xFFFFFF

            val request = SubjectUpdateRequest(
                name = state.name,
                description = state.description.ifBlank { null },
                groupId = state.selectedGroupId,
                color = String.format("#%06X", rgb)
            )

            apiService.updateSubject("Bearer $token", subjectId, request)
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.failure(e)
        }
    }
}

class EditSubjectViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditSubjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditSubjectViewModel(apiService, token, subjectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubjectScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    viewModel: EditSubjectViewModel = viewModel(
        factory = EditSubjectViewModelFactory(apiService, token, subjectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val brandPrimary = Color(0xFF5BA27F)
    val brandPrimaryDeep = Color(0xFF1E4032)

    LaunchedEffect(Unit) { }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "과목 수정",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            color = brandPrimaryDeep
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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

            if (uiState.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("과목 정보를 불러오는 중입니다.")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.96f)
                        ),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = CardDefaults.shape
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "과목 정보를 수정합니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4C6070)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = { viewModel.updateName(it) },
                                label = { Text("과목명") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = { viewModel.updateDescription(it) },
                                label = { Text("설명") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                maxLines = 5
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "그룹 (선택)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            GroupDropdown(
                                groups = uiState.groups,
                                loading = uiState.loadingGroups,
                                selectedGroupId = uiState.selectedGroupId,
                                onGroupSelected = { viewModel.updateGroup(it) },
                                onCreateNewGroup = {
                                    navController.navigate("group/new")
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "색상", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            ColorPaletteRow(
                                colors = uiState.availableColors,
                                selectedColor = uiState.color,
                                onColorSelected = { viewModel.updateColor(it) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = viewModel.submit()
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("과목 수정 완료")
                                                navController.popBackStack()
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    result.exceptionOrNull()?.message
                                                        ?: "과목 수정 실패"
                                                )
                                            }
                                        }
                                    },
                                    enabled = !uiState.submitting && uiState.name.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brandPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (uiState.submitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .height(18.dp)
                                                .padding(end = 8.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    }
                                    Text(text = if (uiState.submitting) "수정 중..." else "수정")
                                }

                                OutlinedButton(
                                    onClick = { navController.popBackStack() },
                                    enabled = !uiState.submitting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("취소")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
