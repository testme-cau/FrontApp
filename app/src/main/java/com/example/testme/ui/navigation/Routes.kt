package com.example.testme.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testme.data.api.ApiService
import com.example.testme.ui.screens.SplashScreen
import com.example.testme.ui.screens.group.DashboardScreen
import com.example.testme.ui.screens.home.HomeScreen
import com.example.testme.ui.screens.login.EmailLoginScreen
import com.example.testme.ui.screens.login.EmailSignupScreen
import com.example.testme.ui.screens.login.LoginScreen
import com.example.testme.ui.screens.subject.SubjectDetailScreen
import com.example.testme.ui.screens.subject.NewSubjectScreen
import com.example.testme.ui.screens.exam.ExamDetailScreen
import com.example.testme.ui.screens.exam.ExamListScreen
import com.example.testme.ui.screens.exam.GenerateExamScreen
import com.example.testme.ui.screens.exam.TakeExamScreen
import com.example.testme.ui.screens.group.NewGroupScreen
import com.example.testme.ui.screens.profile.ProfileScreen
import com.example.testme.ui.screens.subject.EditSubjectScreen
import com.google.android.gms.auth.api.signin.GoogleSignInClient

sealed class Screen(val route: String) {

    data object Splash : Screen("splash")

    data object Login : Screen("login")
    data object EmailLogin : Screen("email_login")
    data object EmailSignup : Screen("email_signup")

    data object NewGroup : Screen("group/new")
    data object EditSubject : Screen("subjects/{subjectId}/edit") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun route(subjectId: String) = "subjects/$subjectId/edit"
    }
    data object Home : Screen("home")

    data object Dashboard : Screen("dashboard")

    data object SubjectList : Screen("groups/{groupId}/subjects") {
        const val ARG_GROUP_ID = "groupId"
        fun route(groupId: String) = "groups/$groupId/subjects"
    }
    data object SubjectDetail : Screen("subjects/{subjectId}/detail") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun route(subjectId: String) = "subjects/$subjectId/detail"
    }

    data object NewSubject : Screen("subjects/new")

    data object ExamList : Screen("subjects/{subjectId}/exams") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun route(subjectId: String) = "subjects/$subjectId/exams"
    }

    data object ExamDetail : Screen("subjects/{subjectId}/exams/{examId}/detail") {
        const val ARG_SUBJECT_ID = "subjectId"
        const val ARG_EXAM_ID = "examId"
        fun route(subjectId: String, examId: String) =
            "subjects/$subjectId/exams/$examId/detail"
    }

    data object TakeExam : Screen("subjects/{subjectId}/exams/{examId}/take") {
        const val ARG_SUBJECT_ID = "subjectId"
        const val ARG_EXAM_ID = "examId"
        fun route(subjectId: String, examId: String) =
            "subjects/$subjectId/exams/$examId/take"
    }

    data object GenerateExam : Screen("subjects/{subjectId}/generate-exam") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun route(subjectId: String) = "subjects/$subjectId/generate-exam"
    }

    data object Profile : Screen("profile")
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    api: ApiService,
    googleSignInClient: GoogleSignInClient,
    token: String,
    activity: Activity
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                googleSignInClient = googleSignInClient
            )
        }

        composable(Screen.EmailLogin.route) {
            EmailLoginScreen(navController = navController)
        }

        composable(Screen.EmailSignup.route) {
            EmailSignupScreen(navController = navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                apiService = api,
                token = token
            )
        }

        composable(
            route = Screen.SubjectList.route,
            arguments = listOf(
                navArgument(Screen.SubjectList.ARG_GROUP_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val groupId =
                backStackEntry.arguments?.getString(Screen.SubjectList.ARG_GROUP_ID)
                    ?: return@composable

            SubjectDetailScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = groupId,
                activity = activity
            )
        }

        composable(
            route = Screen.EditSubject.route,
            arguments = listOf(
                navArgument(Screen.EditSubject.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.EditSubject.ARG_SUBJECT_ID)
                    ?: return@composable

            EditSubjectScreen(
                navController = navController,
                apiService = api,
                token = token.orEmpty(),
                subjectId = subjectId
            )
        }


        composable("group/new") {
            NewGroupScreen(
                navController = navController,
                apiService = api,
                token = token
            )
        }


        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(
                navArgument(Screen.SubjectDetail.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.SubjectDetail.ARG_SUBJECT_ID)
                    ?: return@composable

            SubjectDetailScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = subjectId,
                activity =activity
            )
        }

        composable(Screen.NewSubject.route) {
            NewSubjectScreen(
                navController = navController,
                apiService = api,
                token = token
            )
        }

        composable(
            route = Screen.ExamList.route,
            arguments = listOf(
                navArgument(Screen.ExamList.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.ExamList.ARG_SUBJECT_ID)
                    ?: return@composable

            ExamListScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = subjectId,
            )
        }

        composable(
            route = Screen.ExamDetail.route,
            arguments = listOf(
                navArgument(Screen.ExamDetail.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                },
                navArgument(Screen.ExamDetail.ARG_EXAM_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.ExamDetail.ARG_SUBJECT_ID)
                    ?: return@composable
            val examId =
                backStackEntry.arguments?.getString(Screen.ExamDetail.ARG_EXAM_ID)
                    ?: return@composable

            ExamDetailScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = subjectId,
                examId = examId
            )
        }

        composable(
            route = Screen.TakeExam.route,
            arguments = listOf(
                navArgument(Screen.TakeExam.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                },
                navArgument(Screen.TakeExam.ARG_EXAM_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.TakeExam.ARG_SUBJECT_ID)
                    ?: return@composable
            val examId =
                backStackEntry.arguments?.getString(Screen.TakeExam.ARG_EXAM_ID)
                    ?: return@composable

            TakeExamScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = subjectId,
                examId = examId
            )
        }

        composable(
            route = Screen.GenerateExam.route,
            arguments = listOf(
                navArgument(Screen.GenerateExam.ARG_SUBJECT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subjectId =
                backStackEntry.arguments?.getString(Screen.GenerateExam.ARG_SUBJECT_ID)
                    ?: return@composable

            GenerateExamScreen(
                navController = navController,
                apiService = api,
                token = token,
                subjectId = subjectId
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                apiService = api,
                token=token
            )
        }
    }
}
