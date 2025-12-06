package com.example.testme.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.testme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailLoginScreen(navController: NavController) {

    val auth = Firebase.auth

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutine = rememberCoroutineScope()

    val brandPrimary = Color(0xFF5BA27F)
    val brandPrimaryDeep = Color(0xFF1E4032)
    val brandSecondaryText = Color(0xFF4C6070)
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = stringResource(R.string.email_login_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.97f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFD6F8E6).copy(alpha = 0.7f),
                                        Color.White.copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            stringResource(R.string.app_subtitle_short),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = brandSecondaryText
                            )
                        )
                        Text(
                            "Test.me",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = brandPrimaryDeep
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(R.string.email_label)) },
                            singleLine = true,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password_label)) },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandPrimary,
                                contentColor = Color.White
                            ),
                            onClick = {
                                coroutine.launch {
                                    isLoading = true
                                    errorMessage = ""
                                    try {
                                        auth.signInWithEmailAndPassword(email, password).await()

                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = context.getString(R.string.login_fail_prefix, e.message ?: "")
                                        coroutine.launch {
                                            snackbarHostState.showSnackbar(errorMessage)
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                            Text(if (isLoading) stringResource(R.string.login_btn_loading) else stringResource(R.string.login_btn))
                        }

                        TextButton(
                            onClick = { navController.navigate("email_signup") },
                            enabled = !isLoading,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.signup_link),
                                color = brandSecondaryText
                            )
                        }

                        AnimatedVisibility(
                            visible = errorMessage.isNotEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            Text(
                                errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
