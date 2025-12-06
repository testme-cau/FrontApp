package com.example.testme.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.testme.R
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
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeCard
import com.example.testme.ui.components.TestMeTopAppBar
import com.example.testme.ui.navigation.Screen
import com.example.testme.ui.theme.UIPrimary
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

    fun renameGroup(groupId: String, newName: String, context: android.content.Context) {
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
                    errorMessage = e.message ?: context.getString(R.string.msg_group_update_fail)
                )
            }
        }
    }

    fun deleteGroup(groupId: String, context: android.content.Context) {
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
                    errorMessage = e.message ?: context.getString(R.string.msg_group_delete_fail)
                )
            }
        }
    }

    fun loadSubjects(forceRefresh: Boolean = false, context: android.content.Context? = null) {
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
                val msg = e.message ?: (context?.getString(R.string.msg_subject_list_load_fail) ?: "Failed to load subjects")
                _uiState.value = _uiState.value.copy(
                    loadingSubjects = false,
                    errorMessage = msg
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

    fun confirmDeleteSubject(context: android.content.Context) {
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
                    errorMessage = e.message ?: context.getString(R.string.msg_subject_delete_fail),
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
    val context = LocalContext.current

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

    // Pull to Refresh State
    val isRefreshing = uiState.loadingSubjects || uiState.loadingGroups
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.dashboard_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    GroupSelector(
                        groups = uiState.groups,
                        selectedGroupId = uiState.selectedGroupId,
                        onSelectGroup = { viewModel.selectGroup(it) },
                        onCreateNewGroup = { navController.navigate(Screen.NewGroup.route) },
                        onRenameGroup = { id, name -> viewModel.renameGroup(id, name, context) },
                        onDeleteGroup = { id -> viewModel.deleteGroup(id, context) }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.NewSubject.route) },
                containerColor = UIPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_add_subject),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SoftBlobBackground()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadAll(forceRefresh = true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = pullRefreshState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Section removed (moved to TopAppBar)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredSubjects.isEmpty() && !isRefreshing) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TestMeCard(
                                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.msg_no_subjects),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Button(onClick = { navController.navigate(Screen.NewSubject.route) }) {
                                        Text(stringResource(R.string.action_create_first_subject))
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 100.dp) // FAB Space
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
                        }
                    }
                }
            }

            if (showErrorDialog && uiState.errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text(stringResource(R.string.title_error)) },
                    text = { Text(uiState.errorMessage ?: "") },
                    confirmButton = {
                        Button(onClick = { showErrorDialog = false }) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                )
            }

            if (uiState.deletingSubjectId != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelDeleteSubject() },
                    title = { Text(stringResource(R.string.title_delete_subject)) },
                    text = { Text(stringResource(R.string.msg_delete_subject_confirm)) },
                    confirmButton = {
                        Button(onClick = { viewModel.confirmDeleteSubject(context) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { viewModel.cancelDeleteSubject() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GroupSelector(
    groups: List<GroupData>,
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

    val selectedLabel = selectedGroup?.name ?: stringResource(R.string.label_all_groups)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Card(
                onClick = { dropdownExpanded = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        tint = Color.Gray
                    )
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_all_groups)) },
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
                    text = { Text(stringResource(R.string.action_new_group)) },
                    onClick = {
                        dropdownExpanded = false
                        onCreateNewGroup()
                    }
                )
            }
        }

        if (selectedGroup != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.desc_group_settings)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuExpanded = false
                            renameText = selectedGroup.name
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showRenameDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.title_rename_group)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.label_group_name)) },
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
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.title_delete_group)) },
            text = {
                Text(stringResource(R.string.msg_delete_group_confirm, selectedGroup.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(selectedGroup.groupId)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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

    TestMeCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.desc_more_options),
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            if (!subject.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subject.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!subject.groupId.isNullOrBlank()) {
                    Text(
                        text = subject.groupId ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Enhanced Stats with larger font
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_pdf_count) + " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${subject.pdfCount ?: 0}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_exam_count) + " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${subject.examCount ?: 0}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
