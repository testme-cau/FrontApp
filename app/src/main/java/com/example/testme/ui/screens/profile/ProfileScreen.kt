package com.example.testme.ui.screens.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.LanguageListResponse
import com.example.testme.ui.screens.home.SoftBlobBackground
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val language: String = "ko",
    val availableLanguages: List<String> = emptyList(),
    val loadingLanguages: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val apiService: ApiService,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        val user = Firebase.auth.currentUser
        _uiState.value = _uiState.value.copy(
            displayName = user?.displayName ?: "",
            email = user?.email ?: "",
            language = "ko"
        )
        loadLanguages()
    }

    private fun loadLanguages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingLanguages = true)
            try {
                val response: LanguageListResponse = apiService.getSupportedLanguages()
                val list: List<String> = response.languages.map { it.code }
                val current = _uiState.value.language.ifBlank { "ko" }
                val safeLang = if (list.contains(current)) current else list.firstOrNull() ?: "ko"
                _uiState.value = _uiState.value.copy(
                    availableLanguages = list,
                    language = safeLang,
                    loadingLanguages = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingLanguages = false)
            }
        }
    }

    fun toggleEdit() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isEditing = !current.isEditing,
            errorMessage = null
        )
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun updateLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(language = lang)
    }

    fun cancelEdit() {
        val user = Firebase.auth.currentUser
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            isSaving = false,
            displayName = user?.displayName ?: "",
            language = _uiState.value.language.ifBlank { "ko" },
            errorMessage = null
        )
    }

    fun saveChanges() {
        val state = _uiState.value
        val user = Firebase.auth.currentUser
        if (user == null) {
            _uiState.value = state.copy(
                isEditing = false,
                isSaving = false
            )
            return
        }

        val newName = state.displayName.trim()
        val newLang = state.language.ifBlank { "ko" }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        val profileUpdates = userProfileChangeRequest {
            displayName = newName.ifBlank { null }
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                viewModelScope.launch {
                    try {
                        // apiService.updateUserProfile("Bearer $token", UserProfileUpdateRequest(language = newLang))
                    } catch (_: Exception) {
                    } finally {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            isEditing = false,
                            displayName = newName,
                            language = newLang
                        )
                    }
                }
            } else {
                val msg = task.exception?.message ?: "프로필 저장에 실패했습니다."
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = msg
                )
            }
        }
    }

    fun logout() {
        Firebase.auth.signOut()
    }
}

class ProfileViewModelFactory(
    private val apiService: ApiService,
    private val token: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(apiService, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(apiService, token)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastError by remember { mutableStateOf<String?>(null) }

    val brandPrimary = Color(0xFF5BA27F)
    val brandPrimaryDeep = Color(0xFF1E4032)

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && uiState.errorMessage != lastError) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: "")
            lastError = uiState.errorMessage
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "프로필",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = brandPrimaryDeep
                        )
                    )
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "저장"
                            )
                        }
                        TextButton(
                            onClick = { viewModel.cancelEdit() },
                            enabled = !uiState.isSaving
                        ) {
                            Text("취소")
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "편집"
                            )
                        }
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.96f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = CardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .height(80.dp)
                                .fillMaxWidth(fraction = 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "프로필 아이콘",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.height(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isEditing) {
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = { viewModel.updateDisplayName(it) },
                                label = { Text("이름") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = uiState.displayName.ifBlank { "이름 없음" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileRow(
                                label = "이메일",
                                value = uiState.email.ifBlank { "알 수 없음" }
                            )

                            if (uiState.isEditing) {
                                LanguageDropdown(
                                    label = "언어",
                                    selected = uiState.language.ifBlank { "ko" },
                                    options = uiState.availableLanguages.ifEmpty { listOf("ko", "en") },
                                    loading = uiState.loadingLanguages,
                                    onSelect = { viewModel.updateLanguage(it) }
                                )
                            } else {
                                ProfileRow(
                                    label = "언어",
                                    value = uiState.language.ifBlank { "ko" }.uppercase()
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.logout()
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.95f),
                            contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "로그아웃"
                        )
                        Spacer(modifier = Modifier.weight(1f, false))
                        Text("로그아웃")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun LanguageDropdown(
    label: String,
    selected: String,
    options: List<String>,
    loading: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box {
            OutlinedTextField(
                value = if (loading) "언어 로딩 중..." else selected.uppercase(),
                onValueChange = {},
                readOnly = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = !loading) {
                        expanded = true
                    }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(code.uppercase()) },
                        onClick = {
                            onSelect(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
