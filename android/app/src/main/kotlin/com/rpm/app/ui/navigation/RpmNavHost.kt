package com.rpm.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rpm.app.ui.feature.alerts.AlertsScreen
import com.rpm.app.ui.feature.auth.*
import com.rpm.app.ui.feature.chat.*
import com.rpm.app.ui.feature.notifications.NotificationsScreen
import com.rpm.app.ui.feature.patients.*

object Routes {
    const val LOGIN               = "login"
    const val REGISTER            = "register"
    const val PATIENT_LIST        = "patients"
    const val PATIENT_DETAIL      = "patients/{patientId}"
    const val MY_VITALS           = "my-vitals/{patientId}"
    const val ALERTS              = "alerts?patientId={patientId}"
    const val CONVERSATION_LIST   = "conversations"
    const val CHAT_ROOM           = "conversations/{conversationId}"
    const val NOTIFICATIONS       = "notifications"

    fun patientDetail(id: String) = "patients/$id"
    fun myVitals(id: String) = "my-vitals/$id"
    fun alerts(patientId: String? = null) = if (patientId != null) "alerts?patientId=$patientId" else "alerts"
    fun chatRoom(conversationId: String) = "conversations/$conversationId"
}

@Composable
fun RpmNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    val startDest = when {
        !authState.isLoggedIn -> Routes.LOGIN
        authState.userRole.equals("Patient", ignoreCase = true) && authState.userId != null ->
            Routes.myVitals(authState.userId!!)
        else -> Routes.PATIENT_LIST
    }

    NavHost(navController, startDestination = startDest) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    val state = authViewModel.uiState.value
                    val dest = if (state.userRole.equals("Patient", ignoreCase = true) && state.userId != null) {
                        Routes.myVitals(state.userId)
                    } else {
                        Routes.PATIENT_LIST
                    }
                    navController.navigate(dest) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    val state = authViewModel.uiState.value
                    val dest = if (state.userRole.equals("Patient", ignoreCase = true) && state.userId != null) {
                        Routes.myVitals(state.userId)
                    } else {
                        Routes.PATIENT_LIST
                    }
                    navController.navigate(dest) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.PATIENT_LIST) {
            PatientListScreen(
                onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MY_VITALS,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            PatientDetailScreen(
                onBack = {},
                showBack = false,
                onOpenChat = { navController.navigate(Routes.CONVERSATION_LIST) },
                onOpenAlerts = { pid -> navController.navigate(Routes.alerts(pid)) }
            )
        }

        composable(
            route = Routes.PATIENT_DETAIL,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            PatientDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Routes.CONVERSATION_LIST) },
                onOpenAlerts = { pid -> navController.navigate(Routes.alerts(pid)) }
            )
        }

        composable(
            route = Routes.ALERTS,
            arguments = listOf(navArgument("patientId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) {
            AlertsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CONVERSATION_LIST) {
            ConversationListScreen(
                onConversationClick = { navController.navigate(Routes.chatRoom(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            ChatRoomScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
    }
}
