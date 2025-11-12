package com.example.testme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.testme.ui.screens.*
import com.example.testme.ui.theme.TestMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestMeTheme {
                TestMeApp()
            }
        }
    }
}

@Composable
fun TestMeApp() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login"){LoginScreen(navController)}
        composable("home") { HomeScreen(navController) }
        composable("upload") { UploadScreen(navController) }
        composable("settings") { ExamSettingsScreen(navController) }
        composable("exam") { ExamScreen(navController) }
        composable("result") { ResultScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}
