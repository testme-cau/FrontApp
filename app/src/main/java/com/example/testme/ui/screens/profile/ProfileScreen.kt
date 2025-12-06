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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.testme.R
import androidx.compose.ui.res.stringResource
import com.example.testme.data.model.LanguageListResponse
import com.example.testme.data.api.ApiService
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
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
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!currentLocales.isEmpty) currentLocales[0]?.language ?: "ko" else "ko"

        _uiState.value = _uiState.value.copy(
            displayName = user?.displayName ?: "",
            email = user?.email ?: "",
            language = currentLang,
            availableLanguages = listOf("ko", "en")
        )
    }

    private fun loadLanguages() {
       // No-op: hardcoded in init
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
        val localeList = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(localeList)
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
                val msg = task.exception?.message ?: "Failed to save profile"
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

    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && uiState.errorMessage != lastError) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: "")
            lastError = uiState.errorMessage
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.profile_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.action_save)
                            )
                        }
                        TextButton(
                            onClick = { viewModel.cancelEdit() },
                            enabled = !uiState.isSaving
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit)
                            )
                        }
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
            SoftBlobBackground()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
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
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (uiState.isEditing) {
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = { viewModel.updateDisplayName(it) },
                                label = { Text(stringResource(R.string.label_name)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                singleLine = true
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.label_name),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.displayName.ifBlank { stringResource(R.string.no_name) },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileRow(
                                label = stringResource(R.string.label_email),
                                value = uiState.email.ifBlank { stringResource(R.string.unknown) }
                            )

                            if (uiState.isEditing) {
                                LanguageDropdown(
                                    label = stringResource(R.string.label_language),
                                    selected = uiState.language.ifBlank { "ko" },
                                    options = uiState.availableLanguages.ifEmpty { listOf("ko", "en") },
                                    loading = uiState.loadingLanguages,
                                    onSelect = { viewModel.updateLanguage(it) }
                                )
                            } else {
                                val displayLang = when(uiState.language.ifBlank { "ko" }) {
                                    "ko" -> "🇰🇷 " + stringResource(R.string.language_ko)
                                    "en" -> "🇺🇸 " + stringResource(R.string.language_en)
                                    else -> uiState.language
                                }
                                ProfileRow(
                                    label = stringResource(R.string.label_language),
                                    value = displayLang
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
                            contentDescription = stringResource(R.string.action_logout)
                        )
                        Spacer(modifier = Modifier.weight(1f, false))
                        Text(stringResource(R.string.action_logout))
                    }
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
            val displaySelected = when(selected) {
                "ko" -> "🇰🇷 " + stringResource(R.string.language_ko)
                "en" -> "🇺🇸 " + stringResource(R.string.language_en)
                else -> selected
            }
            OutlinedTextField(
                value = if (loading) stringResource(R.string.loading_languages) else displaySelected,
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
                    val text = when(code) {
                        "ko" -> "🇰🇷 " + stringResource(R.string.language_ko)
                        "en" -> "🇺🇸 " + stringResource(R.string.language_en)
                        else -> code
                    }
                    DropdownMenuItem(
                        text = { Text(text) },
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
