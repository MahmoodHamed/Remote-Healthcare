package com.rpm.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rpm.app.ui.feature.alerts.AlertsScreen
import com.rpm.app.ui.feature.auth.*
import com.rpm.app.ui.feature.chat.*
import com.rpm.app.ui.feature.patients.*

object Routes {
    const val LOGIN               = "login"
    const val REGISTER            = "register"
    const val PATIENT_LIST        = "patients"
    const val PATIENT_DETAIL      = "patients/{patientId}"
    const val ALERTS              = "alerts?patientId={patientId}"
    const val CONVERSATION_LIST   = "conversations"
    const val CHAT_ROOM           = "conversations/{conversationId}"

    fun patientDetail(id: String) = "patients/$id"
    fun alerts(patientId: String? = null) = if (patientId != null) "alerts?patientId=$patientId" else "alerts"
    fun chatRoom(conversationId: String) = "conversations/$conversationId"
}

@Composable
fun RpmNavHost() {
    val navController = rememberNavController()

    // Determine start destination at runtime using AuthViewModel
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    val startDest = when {
        authState.isLoggedIn -> Routes.PATIENT_LIST
        else                 -> Routes.LOGIN
    }

    NavHost(navController, startDestination = startDest) {

        // ── Auth ──────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.PATIENT_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }},
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.PATIENT_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }},
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Patients ──────────────────────────────────────────────────────
        composable(Routes.PATIENT_LIST) {
            PatientListScreen(
                onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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

        // ── Alerts ────────────────────────────────────────────────────────
        composable(
            route = Routes.ALERTS,
            arguments = listOf(navArgument("patientId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) {
            AlertsScreen(onBack = { navController.popBackStack() })
        }

        // ── Chat ──────────────────────────────────────────────────────────
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
    }
}
