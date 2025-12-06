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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.testme.R
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.SubjectCreateRequest
import com.example.testme.data.model.group.GroupData
import com.example.testme.data.model.group.GroupListResponse
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    val brandPrimary = Color(0xFF5BA27F)
    val brandPrimaryDeep = Color(0xFF1E4032)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.title_new_subject),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
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
        ) {
            // 부드러운 배경
            SoftBlobBackground()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
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
                            text = stringResource(R.string.desc_new_subject),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4C6070)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text(stringResource(R.string.label_subject_name)) },
                            placeholder = { Text(stringResource(R.string.hint_subject_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text(stringResource(R.string.label_description)) },
                            placeholder = { Text(stringResource(R.string.hint_subject_desc)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.label_group_optional),
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

                        Text(
                            text = stringResource(R.string.label_color),
                            style = MaterialTheme.typography.labelMedium
                        )
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
                                        val result = viewModel.submit(context)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar(context.getString(R.string.msg_subject_create_success))
                                            navController.popBackStack()
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                result.exceptionOrNull()?.message
                                                    ?: context.getString(R.string.msg_subject_create_fail)
                                            )
                                        }
                                    }
                                },
                                enabled = !uiState.submitting &&
                                        uiState.name.isNotBlank(),
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
                                Text(
                                    text = if (uiState.submitting) stringResource(R.string.action_generating) else stringResource(R.string.action_create_subject)
                                )
                            }

                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                enabled = !uiState.submitting,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    }
                }
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

    val selectedGroupName = remember(groups, selectedGroupId) {
        groups.firstOrNull { it.groupId == selectedGroupId }?.name ?: ""
    }
    // Use stringResource directly inside logic or Text composable if possible, 
    // but here it's inside remember block which might not update on config change if not keyed correctly.
    // Actually stringResource is composable, so we can't use it in remember easily unless we pass context or just use it in Text.
    // Let's handle empty string in Text.

    val noGroupString = stringResource(R.string.label_no_group)
    val displayGroupName = if (selectedGroupName.isBlank()) noGroupString else selectedGroupName

    Column {
        Box {
            OutlinedTextField(
                value = displayGroupName,
                onValueChange = {},
                enabled = false,
                label = { Text(if (loading) stringResource(R.string.label_group_loading) else stringResource(R.string.label_select_group)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading) {
                        expanded = true
                    }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_no_group)) },
                    onClick = {
                        onGroupSelected(null)
                        expanded = false
                    }
                )
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            onGroupSelected(group.groupId)
                            expanded = false
                        }
                    )
                }
                Divider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_create_new_group_plus)) },
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
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isSelected) {
                    Text(
                        text = stringResource(R.string.label_selected),
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

    suspend fun submit(context: android.content.Context): Result<Unit> {
        if (_uiState.value.name.isBlank()) {
            return Result.failure(IllegalArgumentException(context.getString(R.string.err_subject_name_required)))
        }
        _uiState.value = _uiState.value.copy(submitting = true)
        return try {
            val request = SubjectCreateRequest(
                name = _uiState.value.name,
                description = _uiState.value.description.ifBlank { null },
                groupId = _uiState.value.selectedGroupId,
                color = String.format(
                    "#%06X",
                    (_uiState.value.color.value.toInt() and 0xFFFFFF)
                )
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
