package com.example.testme.ui.screens.group

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.SubjectData
import com.example.testme.data.model.SubjectListResponse
import com.example.testme.data.model.group.GroupData
import com.example.testme.data.model.group.GroupListResponse
import com.example.testme.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val subjects: List<SubjectData> = emptyList(),
    val groups: List<GroupData> = emptyList(),
    val selectedGroupId: String? = null,
    val loadingSubjects: Boolean = false,
    val loadingGroups: Boolean = false,
    val errorMessage: String? = null,
    val deletingSubjectId: String? = null
)

class DashboardViewModel(
    private val apiService: ApiService,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    fun loadAll(forceRefresh: Boolean = false) {
        loadSubjects(forceRefresh)
        loadGroups(forceRefresh)
    }

    fun loadSubjects(forceRefresh: Boolean = false) {
        if (_uiState.value.loadingSubjects && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingSubjects = true, errorMessage = null)
            try {
                val response: SubjectListResponse = apiService.getAllSubjects("Bearer $token")
                _uiState.value = _uiState.value.copy(
                    subjects = response.subjects,
                    loadingSubjects = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadingSubjects = false,
                    errorMessage = e.message ?: "과목 목록을 불러오지 못했습니다."
                )
            }
        }
    }

    fun loadGroups(forceRefresh: Boolean = false) {
        if (_uiState.value.loadingGroups && !forceRefresh) return
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

    fun selectGroup(groupId: String?) {
        _uiState.value = _uiState.value.copy(selectedGroupId = groupId)
    }

    fun requestDeleteSubject(subjectId: String) {
        _uiState.value = _uiState.value.copy(deletingSubjectId = subjectId)
    }

    fun cancelDeleteSubject() {
        _uiState.value = _uiState.value.copy(deletingSubjectId = null)
    }

    fun confirmDeleteSubject() {
        val id = _uiState.value.deletingSubjectId ?: return
        viewModelScope.launch {
            try {
                // ApiService에 맞게 실제 삭제 API 연결
                // apiService.deleteSubject("Bearer $token", id)
                _uiState.value = _uiState.value.copy(
                    subjects = _uiState.value.subjects.filterNot { it.subjectId == id },
                    deletingSubjectId = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "과목 삭제에 실패했습니다.",
                    deletingSubjectId = null
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val apiService: ApiService,
    private val token: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(apiService, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(apiService, token)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showErrorDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            showErrorDialog = true
        }
    }

    val filteredSubjects = remember(uiState.subjects, uiState.selectedGroupId) {
        val groupId = uiState.selectedGroupId
        if (groupId.isNullOrBlank()) uiState.subjects
        else uiState.subjects.filter { it.groupId == groupId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("대시보드") },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAll(forceRefresh = true) },
                        enabled = !uiState.loadingSubjects && !uiState.loadingGroups
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewSubject.route) }
            ) {
                Text(text = "새 과목 생성")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            GroupFilterDropdown(
                groups = uiState.groups,
                loading = uiState.loadingGroups,
                selectedGroupId = uiState.selectedGroupId,
                onSelectGroup = { viewModel.selectGroup(it) },
                onCreateNewGroup = {
                    navController.navigate(Screen.NewGroup.route)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.loadingSubjects && filteredSubjects.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredSubjects.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "등록된 과목이 없습니다.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { navController.navigate(Screen.NewSubject.route) }) {
                                Text("첫 과목 만들기")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSubjects) { subject ->
                            SubjectCard(
                                subject = subject,
                                onClick = {
                                    navController.navigate(
                                        Screen.SubjectDetail.route(subject.subjectId)
                                    )
                                },
                                onEditClick = {
                                    navController.navigate(
                                        Screen.EditSubject.route(subject.subjectId)
                                    )
                                },
                                onDeleteClick = {
                                    viewModel.requestDeleteSubject(subject.subjectId)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        if (showErrorDialog && uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("오류") },
                text = { Text(uiState.errorMessage ?: "") },
                confirmButton = {
                    Button(onClick = { showErrorDialog = false }) {
                        Text("확인")
                    }
                }
            )
        }

        if (uiState.deletingSubjectId != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteSubject() },
                title = { Text("과목 삭제") },
                text = { Text("정말 이 과목을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
                confirmButton = {
                    Button(onClick = { viewModel.confirmDeleteSubject() }) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.cancelDeleteSubject() }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun GroupFilterDropdown(
    groups: List<GroupData>,
    loading: Boolean,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onCreateNewGroup: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = remember(groups, selectedGroupId) {
        if (selectedGroupId.isNullOrBlank()) "전체"
        else groups.firstOrNull { it.groupId == selectedGroupId }?.name ?: "전체"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "그룹",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading) { expanded = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (loading) "그룹 로딩 중..." else selectedLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("전체") },
                    onClick = {
                        onSelectGroup(null)
                        expanded = false
                    }
                )
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            onSelectGroup(group.groupId)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("＋ 새 그룹 만들기") },
                    onClick = {
                        expanded = false
                        onCreateNewGroup()
                    }
                )
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectData,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(subject.color ?: "#4F46E5"))
    } catch (e: IllegalArgumentException) {
        Color(0xFF4F46E5)
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.02f)
                    ) {
                        drawRoundRect(color = color)
                    }
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            if (!subject.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subject.description ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!subject.groupId.isNullOrBlank()) {
                    Text(
                        text = subject.groupId ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (subject.pdfCount != null) {
                    Text(
                        text = "PDF ${subject.pdfCount}개",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (subject.examCount != null) {
                    Text(
                        text = "시험 ${subject.examCount}개",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
