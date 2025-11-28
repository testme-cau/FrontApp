@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.testme.ui.screens.login

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.testme.R
import com.example.testme.ui.screens.home.SoftBlobBackground
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    navController: NavController,
    googleSignInClient: GoogleSignInClient
) {
    val coroutine = rememberCoroutineScope()
    val auth = Firebase.auth
    var errorMessage by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser = auth.currentUser
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        coroutine.launch {
            try {
                isGoogleLoading = true
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).await()

                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            } catch (e: ApiException) {
                isGoogleLoading = false
                errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                        "사용자가 Google 로그인을 취소했어요."
                    else ->
                        "Google 로그인 실패 (code=${e.statusCode}): ${e.message}"
                }
                snackbarHostState.showSnackbar(errorMessage)
            } catch (e: Exception) {
                isGoogleLoading = false
                errorMessage = "Google 로그인 실패: ${e.message}"
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    val brandPrimary = Color(0xFF5BA27F)
    val brandPrimaryDeep = Color(0xFF1E4032)
    val brandSecondaryText = Color(0xFF4C6070)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "로그인",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = brandPrimaryDeep
                        )
                    )
                },
                navigationIcon = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
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
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Test.me",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = brandPrimaryDeep
                    )
                )
                Text(
                    text = "AI 기반 맞춤형 시험 생성 플랫폼",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = brandSecondaryText
                    )
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.97f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "로그인 방법을 선택해 주세요",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = brandSecondaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { navController.navigate("email_login") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !isGoogleLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("이메일 / 비밀번호 로그인")
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                Log.d("AuthDebug", "PACKAGE=${context.packageName}")
                                errorMessage = ""
                                launcher.launch(googleSignInClient.signInIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !isGoogleLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = brandSecondaryText
                            ),
                            border = BorderStroke(1.dp, brandSecondaryText)
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isGoogleLoading) "Google 로그인 중..." else "Google 계정으로 계속하기")
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
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
