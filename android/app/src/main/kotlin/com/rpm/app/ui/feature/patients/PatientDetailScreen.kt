package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    userRole: String?,
    onBack: () -> Unit,
    onOpenChat: (conversationId: String) -> Unit,
    onOpenAlerts: (patientId: String) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val patient = uiState.patient
    val vitalsTitle = vitalsSectionTitle(userRole, patient?.fullName)
    val showBack = userRole != "Patient"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (userRole) {
                            "Patient" -> "My Health"
                            else -> patient?.fullName ?: "Patient Detail"
                        },
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { patient?.let { onOpenAlerts(it.userId) } }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                    }
                    if (userRole == "Doctor") {
                        IconButton(
                            onClick = {
                                viewModel.openDoctorChat(onConversationReady = onOpenChat)
                            },
                            enabled = !uiState.isOpeningChat && patient != null,
                        ) {
                            if (uiState.isOpeningChat) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Chat, contentDescription = "New Chat")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null && patient == null -> Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                patient != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        VitalsDetailCard(
                            title = vitalsTitle,
                            live = uiState.realtimeVitals,
                            latest = patient.latestVitals,
                            full = uiState.latestVitals,
                        )

                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    when (userRole) {
                                        "Patient" -> "My Profile"
                                        "Relative" -> "Family Member"
                                        else -> "Patient Information"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(8.dp))
                                VitalRow("Name", patient.fullName)
                                patient.bloodType?.let { VitalRow("Blood Type", it) }
                                patient.doctor?.let {
                                    VitalRow("Doctor", it.fullName)
                                    it.specialization?.let { s -> VitalRow("Specialization", s) }
                                }
                                if (userRole == "Doctor") {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.openDoctorChat(onConversationReady = onOpenChat) },
                                        enabled = !uiState.isOpeningChat,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("New Chat")
                                    }
                                }
                            }
                        }

                        uiState.error?.let { msg ->
                            Text(msg, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
