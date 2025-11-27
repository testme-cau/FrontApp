package com.example.testme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.testme.data.api.ApiService
import com.example.testme.ui.navigation.AppNavHost
import com.example.testme.ui.theme.TestMeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleSignInClient = createGoogleSignInClient()
        val apiService = ApiService.create()

        setContent {
            TestMeTheme {
                val auth = Firebase.auth
                var token by remember { mutableStateOf("") }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        val current = firebaseAuth.currentUser
                        if (current == null) {
                            token = ""
                        } else {
                            current.getIdToken(false)
                                .addOnSuccessListener { result ->
                                    token = result.token ?: ""
                                }
                                .addOnFailureListener {
                                    token = ""
                                }
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                AppNavHost(
                    api = apiService,
                    googleSignInClient = googleSignInClient,
                    token = token,
                    activity = this
                )
            }
        }
    }

    private fun createGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        return GoogleSignIn.getClient(this, gso)
    }
}
