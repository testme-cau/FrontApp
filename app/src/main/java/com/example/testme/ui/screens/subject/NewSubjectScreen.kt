package com.example.testme.ui.screens.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.SubjectCreateRequest
import com.example.testme.data.model.group.GroupData
import com.example.testme.data.model.group.GroupListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSubjectScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    viewModel: NewSubjectViewModel = viewModel(
        factory = NewSubjectViewModelFactory(apiService, token)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "새 과목 추가") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "과목 정보를 입력해서 새로운 과목을 만들어 보세요.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("과목명") },
                placeholder = { Text("예: 데이터베이스") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("설명") },
                placeholder = { Text("과목에 대한 간단한 설명") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "그룹 (선택)", style = MaterialTheme.typography.labelMedium)
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
                                snackbarHostState.showSnackbar("과목 생성 완료")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar(
                                    result.exceptionOrNull()?.message ?: "과목 생성 실패"
                                )
                            }
                        }
                    },
                    enabled = !uiState.submitting && uiState.name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = if (uiState.submitting) "생성 중..." else "과목 생성")
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    enabled = !uiState.submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "취소")
                }
            }
        }
    }
}

@Composable
fun GroupDropdown(
    groups: List<GroupData>,
    loading: Boolean,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateNewGroup: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = groups.firstOrNull { it.groupId == selectedGroupId }?.name ?: ""
            )
        )
    }

    Column {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            enabled = false,
            label = { Text(if (loading) "그룹 로딩 중..." else "그룹 선택") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !loading) {
                    expanded = !expanded
                }
        )

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("그룹 없음") },
                onClick = {
                    onGroupSelected(null)
                    textFieldValue = TextFieldValue("")
                    expanded = false
                }
            )
            groups.forEach { group ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                        onGroupSelected(group.groupId)
                        textFieldValue = TextFieldValue(group.name)
                        expanded = false
                    }
                )
            }
            Divider()
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("＋ 새 그룹 생성") },
                onClick = {
                    expanded = false
                    onCreateNewGroup()
                }
            )
        }
    }
}

@Composable
fun ColorPaletteRow(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onColorSelected(color) }
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isSelected) {
                    Text(
                        text = "선택",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


data class NewSubjectUiState(
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
    val loadingGroups: Boolean = false,
    val submitting: Boolean = false
)

class NewSubjectViewModel(
    private val apiService: ApiService,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewSubjectUiState())
    val uiState: StateFlow<NewSubjectUiState> = _uiState

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

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingGroups = true)
            try {
                val response: GroupListResponse = apiService.getGroups("Bearer $token")
                _uiState.value = _uiState.value.copy(
                    groups = response.groups,
                    loadingGroups = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingGroups = false)
            }
        }
    }

    suspend fun submit(): Result<Unit> {
        if (_uiState.value.name.isBlank()) {
            return Result.failure(IllegalArgumentException("과목명을 입력해주세요."))
        }
        _uiState.value = _uiState.value.copy(submitting = true)
        return try {
            val request = SubjectCreateRequest(
                name = _uiState.value.name,
                description = _uiState.value.description.ifBlank { null },
                groupId = _uiState.value.selectedGroupId,
                color = String.format("#%06X", (_uiState.value.color.value.toInt() and 0xFFFFFF))
            )
            apiService.createSubject("Bearer $token", request)
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(submitting = false)
            Result.failure(e)
        }
    }
}

class NewSubjectViewModelFactory(
    private val apiService: ApiService,
    private val token: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewSubjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewSubjectViewModel(apiService, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
