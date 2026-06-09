package com.rpm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rpm.app.ui.RequestNotificationPermission
import com.rpm.app.ui.feature.auth.*
import com.rpm.app.ui.feature.chat.*

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val CHAT_ROOM = "conversations/{conversationId}"

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
fun RpmNavHost(
    initialConversationId: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    var wasLoggedIn by remember { mutableStateOf(false) }

    if (authState.isLoggedIn) {
        RequestNotificationPermission()
    }

    // Session-expired dialog — only on login screen, never while authenticated
    if (authState.showSessionExpiredDialog && !authState.isLoggedIn) {
        AlertDialog(
            onDismissRequest = { authViewModel.dismissSessionExpiredDialog() },
            icon             = { Icon(Icons.Default.Lock, contentDescription = null) },
            title            = { Text("Session Expired") },
            text             = { Text("Your session has expired. Please sign in again to continue.") },
            confirmButton    = {
                Button(onClick = { authViewModel.dismissSessionExpiredDialog() }) {
                    Text("Sign In")
                }
            },
        )
    }

    LaunchedEffect(authState.isLoggedIn, authState.isSessionReady, initialConversationId) {
        if (!authState.isSessionReady || !authState.isLoggedIn) return@LaunchedEffect
        if (!initialConversationId.isNullOrBlank()) {
            navController.navigate(Routes.chatRoom(initialConversationId)) {
                launchSingleTop = true
            }
            onDeepLinkConsumed()
        }
    }

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
            route = Routes.CHAT_ROOM,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ChatRoomScreen(onBack = { navController.popBackStack() })
        }
    }
}
