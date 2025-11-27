package com.example.testme.ui.screens.group

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.group.GroupCreateRequest
import kotlinx.coroutines.launch

data class NewGroupUiState(
    val name: String = "",
    val description: String = "",
    val submitting: Boolean = false
)

class NewGroupViewModel(
    private val apiService: ApiService,
    private val token: String
) : ViewModel() {

    var uiState by mutableStateOf(NewGroupUiState())
        private set

    fun updateName(value: String) {
        uiState = uiState.copy(name = value)
    }

    fun updateDescription(value: String) {
        uiState = uiState.copy(description = value)
    }

    suspend fun submit(): Result<Unit> {
        if (uiState.name.isBlank()) {
            return Result.failure(IllegalArgumentException("그룹 이름을 입력해주세요."))
        }
        uiState = uiState.copy(submitting = true)
        return try {
            val request = GroupCreateRequest(
                name = uiState.name,
                description = uiState.description.ifBlank { null }
            )
            apiService.createGroup("Bearer $token", request)
            uiState = uiState.copy(submitting = false)
            Result.success(Unit)
        } catch (e: Exception) {
            uiState = uiState.copy(submitting = false)
            Result.failure(e)
        }
    }
}

class NewGroupViewModelFactory(
    private val apiService: ApiService,
    private val token: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewGroupViewModel(apiService, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    viewModel: NewGroupViewModel = viewModel(
        factory = NewGroupViewModelFactory(apiService, token)
    )
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 그룹 만들기") },
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
            Text("과목을 묶어서 관리할 그룹을 만들어 보세요.")
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("그룹 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("설명 (선택)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
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
                                snackbarHostState.showSnackbar("그룹 생성 완료")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar(
                                    result.exceptionOrNull()?.message ?: "그룹 생성 실패"
                                )
                            }
                        }
                    },
                    enabled = !state.submitting && state.name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = if (state.submitting) "생성 중..." else "그룹 생성")
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    enabled = !state.submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("취소")
                }
            }
        }
    }
}
