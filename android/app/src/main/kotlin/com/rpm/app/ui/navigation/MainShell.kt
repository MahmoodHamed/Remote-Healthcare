package com.rpm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rpm.app.ui.feature.alerts.AlertsScreen
import com.rpm.app.ui.feature.chat.ConversationListScreen
import com.rpm.app.ui.feature.notifications.NotificationsScreen
import com.rpm.app.ui.feature.notifications.NotificationsViewModel
import com.rpm.app.ui.feature.patients.DeviceManagementScreen
import com.rpm.app.ui.feature.patients.LiveMonitorScreen
import com.rpm.app.ui.feature.patients.PatientDetailScreen
import com.rpm.app.ui.feature.patients.PatientListScreen

enum class MainTab { Patients, Messages, Notifications }

private object ShellRoutes {
    const val PATIENT_LIST = "shell/patients"
    const val PATIENT_DETAIL = "shell/patients/{patientId}"
    const val LIVE_MONITOR = "shell/live/{patientId}"
    const val DEVICE_MANAGEMENT = "shell/devices"
    const val ALERTS = "shell/alerts?patientId={patientId}"

    fun patientDetail(id: String) = "shell/patients/$id"
    fun liveMonitor(id: String) = "shell/live/$id"
    fun alerts(patientId: String? = null) =
        if (patientId != null) "shell/alerts?patientId=$patientId" else "shell/alerts"
}

fun mainTabLabel(tab: MainTab, userRole: String?): String = when (tab) {
    MainTab.Patients -> when (userRole) {
        "Patient" -> "Health"
        "Doctor" -> "Patients"
        "Relative" -> "Family"
        else -> "Home"
    }
    MainTab.Messages -> "Messages"
    MainTab.Notifications -> "Alerts"
}

@Composable
fun MainShell(
    navController: NavHostController,
    userRole: String?,
    userId: String?,
    onLogout: () -> Unit,
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(MainTab.Patients) }
    val patientsNavController = rememberNavController()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) { notificationsViewModel.refresh() }

    LaunchedEffect(userRole, userId) {
        if (userRole == "Patient" && !userId.isNullOrBlank()) {
            val route = patientsNavController.currentDestination?.route
            if (route == null || route == ShellRoutes.PATIENT_LIST) {
                patientsNavController.navigate(ShellRoutes.patientDetail(userId)) {
                    popUpTo(ShellRoutes.PATIENT_LIST) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Patients,
                    onClick = { selectedTab = MainTab.Patients },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.Patients) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = mainTabLabel(MainTab.Patients, userRole),
                        )
                    },
                    label = { Text(mainTabLabel(MainTab.Patients, userRole)) },
                    alwaysShowLabel = true,
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Messages,
                    onClick = { selectedTab = MainTab.Messages },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.Messages) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Messages",
                        )
                    },
                    label = { Text("Messages") },
                    alwaysShowLabel = true,
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Notifications,
                    onClick = { selectedTab = MainTab.Notifications },
                    icon = {
                        if (unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text(unreadCount.coerceAtMost(99).toString()) } }) {
                                Icon(
                                    if (selectedTab == MainTab.Notifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "Alerts",
                                )
                            }
                        } else {
                            Icon(
                                if (selectedTab == MainTab.Notifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                            )
                        }
                    },
                    label = { Text("Alerts") },
                    alwaysShowLabel = true,
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Patients -> PatientsTabNav(
                modifier = Modifier.padding(padding),
                patientsNavController = patientsNavController,
                rootNavController = navController,
                userRole = userRole,
                userId = userId,
                onLogout = onLogout,
            )
            MainTab.Messages -> ConversationListScreen(
                modifier = Modifier.padding(padding),
                onConversationClick = { navController.navigate(Routes.chatRoom(it)) },
                onBack = {},
                showBack = false,
            )
            MainTab.Notifications -> Box(Modifier.padding(padding)) {
                NotificationsScreen(
                    showBack = false,
                    viewModel = notificationsViewModel,
                )
            }
        }
    }
}

@Composable
private fun PatientsTabNav(
    modifier: Modifier,
    patientsNavController: NavHostController,
    rootNavController: NavHostController,
    userRole: String?,
    userId: String?,
    onLogout: () -> Unit,
) {
    NavHost(
        navController = patientsNavController,
        startDestination = ShellRoutes.PATIENT_LIST,
        modifier = modifier,
    ) {
        composable(ShellRoutes.PATIENT_LIST) {
            PatientListScreen(
                title = patientListTitle(userRole),
                emptyMessage = patientListEmptyMessage(userRole),
                userRole = userRole,
                onPatientClick = { patientsNavController.navigate(ShellRoutes.patientDetail(it)) },
                onLogout = onLogout,
                autoOpenSinglePatient = false,
            )
        }

        composable(
            route = ShellRoutes.PATIENT_DETAIL,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
        ) {
            PatientDetailScreen(
                userRole = userRole,
                userId = userId,
                onBack = { patientsNavController.popBackStack() },
                onOpenChat = { rootNavController.navigate(Routes.chatRoom(it)) },
                onOpenAlerts = { pid -> patientsNavController.navigate(ShellRoutes.alerts(pid)) },
                onOpenLiveMonitor = { pid -> patientsNavController.navigate(ShellRoutes.liveMonitor(pid)) },
                onOpenDeviceManagement = if (userRole == "Patient") {
                    { patientsNavController.navigate(ShellRoutes.DEVICE_MANAGEMENT) }
                } else null,
            )
        }

        composable(
            route = ShellRoutes.LIVE_MONITOR,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
        ) {
            LiveMonitorScreen(onBack = { patientsNavController.popBackStack() })
        }

        composable(ShellRoutes.DEVICE_MANAGEMENT) {
            DeviceManagementScreen(onBack = { patientsNavController.popBackStack() })
        }

        composable(
            route = ShellRoutes.ALERTS,
            arguments = listOf(
                navArgument("patientId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AlertsScreen(onBack = { patientsNavController.popBackStack() })
        }
    }
}
