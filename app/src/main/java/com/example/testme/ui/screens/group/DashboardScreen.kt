package com.example.testme.ui.screens.group

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.SubjectData
import com.example.testme.data.model.SubjectListResponse
import com.example.testme.data.model.group.GroupData
import com.example.testme.data.model.group.GroupListResponse
import com.example.testme.data.model.group.GroupUpdateRequest
import com.example.testme.ui.navigation.Screen
import com.example.testme.ui.screens.home.SoftBlobBackground
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

    fun renameGroup(groupId: String, newName: String) {
        viewModelScope.launch {
            try {
                val current = _uiState.value
                val target = current.groups.firstOrNull { it.groupId == groupId } ?: return@launch

                val req = GroupUpdateRequest(
                    name = newName,
                    description = target.description,
                    color = target.color
                )

                apiService.updateGroup("Bearer $token", groupId, req)
                loadGroups(forceRefresh = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "그룹 수정에 실패했습니다."
                )
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                apiService.deleteGroup("Bearer $token", groupId)

                val current = _uiState.value
                _uiState.value = current.copy(
                    groups = current.groups.filterNot { it.groupId == groupId },
                    selectedGroupId = if (current.selectedGroupId == groupId) null else current.selectedGroupId,
                    subjects = current.subjects.filterNot { it.groupId == groupId }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "그룹 삭제에 실패했습니다."
                )
            }
        }
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
                apiService.deleteSubject("Bearer $token", id)

                val current = _uiState.value
                _uiState.value = current.copy(
                    subjects = current.subjects.filterNot { it.subjectId == id },
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
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "대시보드",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAll(forceRefresh = true) },
                        enabled = !uiState.loadingSubjects && !uiState.loadingGroups
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = Color(0xFF1E4032)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFF1E4032)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewSubject.route) }
            ) {
                Text(text = "＋", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SoftBlobBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GroupFilterDropdown(
                    groups = uiState.groups,
                    loading = uiState.loadingGroups,
                    selectedGroupId = uiState.selectedGroupId,
                    onSelectGroup = { viewModel.selectGroup(it) },
                    onCreateNewGroup = {
                        navController.navigate(Screen.NewGroup.route)
                    },
                    onRenameGroup = { groupId, newName ->
                        viewModel.renameGroup(groupId, newName)
                    },
                    onDeleteGroup = { groupId ->
                        viewModel.deleteGroup(groupId)
                    }
                )

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
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "등록된 과목이 없습니다.")
                                    Button(onClick = { navController.navigate(Screen.NewSubject.route) }) {
                                        Text("첫 과목 만들기")
                                    }
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
}

@Composable
private fun GroupFilterDropdown(
    groups: List<GroupData>,
    loading: Boolean,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onCreateNewGroup: () -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    val selectedGroup = remember(groups, selectedGroupId) {
        groups.firstOrNull { it.groupId == selectedGroupId }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectedLabel = remember(groups, selectedGroupId) {
        if (selectedGroupId.isNullOrBlank()) "전체 그룹"
        else groups.firstOrNull { it.groupId == selectedGroupId }?.name ?: "전체 그룹"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "그룹 필터",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedGroup != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "그룹 설정"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("이름 수정") },
                            onClick = {
                                menuExpanded = false
                                renameText = selectedGroup.name
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading) { dropdownExpanded = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFD6F8E6).copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("전체 그룹") },
                    onClick = {
                        onSelectGroup(null)
                        dropdownExpanded = false
                    }
                )
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            onSelectGroup(group.groupId)
                            dropdownExpanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("＋ 새 그룹 만들기") },
                    onClick = {
                        dropdownExpanded = false
                        onCreateNewGroup()
                    }
                )
            }
        }
    }

    if (showRenameDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("그룹 이름 수정") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("그룹 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameText.trim()
                        if (newName.isNotEmpty()) {
                            onRenameGroup(selectedGroup.groupId, newName)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showDeleteDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("그룹 삭제") },
            text = {
                Text(
                    "\"${selectedGroup.name}\" 그룹을 삭제하시겠습니까?\n" +
                            "이 그룹에 속한 과목은 그룹이 해제되거나 목록에서 제거될 수 있습니다."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(selectedGroup.groupId)
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
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
    } catch (_: IllegalArgumentException) {
        Color(0xFF4F46E5)
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.97f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .height(18.dp)
                            .fillMaxWidth(0.03f)
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
