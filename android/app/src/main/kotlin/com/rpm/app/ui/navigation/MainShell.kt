package com.rpm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rpm.app.ui.feature.chat.ConversationListScreen
import com.rpm.app.ui.feature.notifications.NotificationsScreen
import com.rpm.app.ui.feature.notifications.NotificationsViewModel
import com.rpm.app.ui.feature.patients.PatientListScreen

enum class MainTab { Patients, Messages, Notifications }

@Composable
fun MainShell(
    navController: NavHostController,
    userRole: String?,
    userId: String?,
    onLogout: () -> Unit,
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
) {
    var selectedTab  by remember { mutableStateOf(MainTab.Patients) }
    val unreadCount  by notificationsViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) { notificationsViewModel.refresh() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Patients,
                    onClick  = { selectedTab = MainTab.Patients },
                    icon     = {
                        Icon(
                            if (selectedTab == MainTab.Patients) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Patients",
                        )
                    },
                    label    = { Text(patientListTitle(userRole)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Messages,
                    onClick  = { selectedTab = MainTab.Messages },
                    icon     = {
                        Icon(
                            if (selectedTab == MainTab.Messages) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Messages",
                        )
                    },
                    label    = { Text("Messages") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Notifications,
                    onClick  = { selectedTab = MainTab.Notifications },
                    icon     = {
                        if (unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text(unreadCount.coerceAtMost(99).toString()) } }) {
                                Icon(
                                    if (selectedTab == MainTab.Notifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                )
                            }
                        } else {
                            Icon(
                                if (selectedTab == MainTab.Notifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                            )
                        }
                    },
                    label    = { Text("Notifications") },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Patients -> PatientListScreen(
                modifier              = Modifier.padding(padding),
                title                 = patientListTitle(userRole),
                emptyMessage          = patientListEmptyMessage(userRole),
                userRole              = userRole,
                onPatientClick        = { navController.navigate(Routes.patientDetail(it)) },
                onLogout              = onLogout,
                autoOpenSinglePatient = userRole == "Patient",
            )
            MainTab.Messages -> ConversationListScreen(
                modifier              = Modifier.padding(padding),
                onConversationClick   = { navController.navigate(Routes.chatRoom(it)) },
                onBack                = {},
                showBack              = false,
            )
            MainTab.Notifications -> Box(Modifier.padding(padding)) {
                NotificationsScreen(
                    showBack  = false,
                    viewModel = notificationsViewModel,
                )
            }
        }
    }
}
