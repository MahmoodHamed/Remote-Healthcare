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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.signalr.RealTimeVitals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onBack: () -> Unit,
    onOpenChat: (patientId: String) -> Unit,
    onOpenAlerts: (patientId: String) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val patient = uiState.patient

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.fullName ?: "Patient Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { patient?.let { onOpenAlerts(it.patientId) } }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                    }
                    IconButton(onClick = { patient?.let { onOpenChat(it.userId) } }) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                patient != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Patient info card
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Patient Information", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Name", patient.fullName)
                                patient.bloodType?.let { InfoRow("Blood Type", it) }
                                patient.doctor?.let {
                                    InfoRow("Doctor", it.fullName)
                                    it.specialization?.let { s -> InfoRow("Specialization", s) }
                                }
                            }
                        }

                        // Real-time vitals (if connected)
                        uiState.realtimeVitals?.let { rv ->
                            RealtimeVitalsCard(rv)
                        } ?: uiState.latestVitals?.let { v ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Latest Vitals", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    v.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm") }
                                    v.spO2Percent?.let { VitalRow("SpO2", "${it.toInt()}%") }
                                    v.temperatureC?.let { VitalRow("Temperature", String.format("%.1f °C", it)) }
                                    if (v.systolicBp != null && v.diastolicBp != null)
                                        VitalRow("Blood Pressure", "${v.systolicBp.toInt()}/${v.diastolicBp.toInt()} mmHg")
                                    VitalRow("Wearing Watch", if (v.isWearing) "Yes" else "No")
                                    if (v.fallDetected)
                                        Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealtimeVitalsCard(rv: RealTimeVitals) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Live Vitals", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            rv.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm") }
            rv.spO2Percent?.let { VitalRow("SpO2", "${it.toInt()}%") }
            rv.temperatureC?.let { VitalRow("Temperature", String.format("%.1f °C", it)) }
            if (rv.systolicBp != null && rv.diastolicBp != null)
                VitalRow("Blood Pressure", "${rv.systolicBp.toInt()}/${rv.diastolicBp.toInt()} mmHg")
            if (rv.fallDetected)
                Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun VitalRow(label: String, value: String) = InfoRow(label, value)
