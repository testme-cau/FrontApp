package com.example.testme.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.testme.data.model.group.GroupCreateRequest
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
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
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = "새 그룹 만들기",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(padding)
        ) {
            SoftBlobBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFD6F8E6).copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "과목을 묶어서 관리할 그룹을 만들어 보세요.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text("그룹 이름") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("설명 (선택)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val result = viewModel.submit()
                                        if (result.isSuccess) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("그룹 생성 완료")
                                            }
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
        }
    }
}
