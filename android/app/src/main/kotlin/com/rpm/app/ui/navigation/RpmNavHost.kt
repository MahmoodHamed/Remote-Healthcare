package com.rpm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rpm.app.ui.feature.alerts.AlertsScreen
import com.rpm.app.ui.feature.auth.*
import com.rpm.app.ui.feature.chat.*
import com.rpm.app.ui.feature.patients.*

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val PATIENT_DETAIL = "patients/{patientId}"
    const val ALERTS = "alerts?patientId={patientId}"
    const val CHAT_ROOM = "conversations/{conversationId}"

    fun patientDetail(id: String) = "patients/$id"
    fun alerts(patientId: String? = null) =
        if (patientId != null) "alerts?patientId=$patientId" else "alerts"
    fun chatRoom(conversationId: String) = "conversations/$conversationId"
}

fun homeRouteForRole(role: String?, userId: String?): String = Routes.MAIN

fun patientListTitle(role: String?): String = when (role) {
    "Doctor" -> "My Patients"
    "Patient" -> "My Health"
    "Relative" -> "Family Members"
    else -> "Remote Patient Monitoring"
}

fun patientListEmptyMessage(role: String?): String = when (role) {
    "Doctor" -> "No patients assigned yet."
    "Patient" -> "Your health profile is not set up yet."
    "Relative" -> "No linked family members yet. Ask a patient to link your account."
    else -> "No records to show."
}

@Composable
fun RpmNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    var wasLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(authState.isLoggedIn, authState.isSessionReady, authState.userRole, authState.userId) {
        if (!authState.isSessionReady) return@LaunchedEffect
        if (authState.isLoggedIn) {
            wasLoggedIn = true
            val dest = homeRouteForRole(authState.userRole, authState.userId)
            val current = navController.currentDestination?.route
            if (current == Routes.LOGIN || current == Routes.REGISTER) {
                navController.navigate(dest) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        } else if (wasLoggedIn) {
            wasLoggedIn = false
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (!authState.isSessionReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navigateHome: () -> Unit = {
        val dest = homeRouteForRole(authState.userRole, authState.userId)
        navController.navigate(dest) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    NavHost(navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navigateHome() },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navigateHome() },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(Routes.MAIN) {
            MainShell(
                navController = navController,
                userRole = authState.userRole,
                userId = authState.userId,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.PATIENT_DETAIL,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
        ) {
            PatientDetailScreen(
                userRole = authState.userRole,
                onBack = { navController.popBackStack() },
                onOpenChat = { conversationId ->
                    navController.navigate(Routes.chatRoom(conversationId))
                },
                onOpenAlerts = { pid -> navController.navigate(Routes.alerts(pid)) },
            )
        }

        composable(
            route = Routes.ALERTS,
            arguments = listOf(
                navArgument("patientId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AlertsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ChatRoomScreen(onBack = { navController.popBackStack() })
        }
    }
}
