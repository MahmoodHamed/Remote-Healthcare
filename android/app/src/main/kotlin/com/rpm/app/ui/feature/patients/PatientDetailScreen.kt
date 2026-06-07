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
import com.rpm.app.data.remote.dto.VitalRecordDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onBack: () -> Unit,
    onOpenChat: (patientId: String) -> Unit,
    onOpenAlerts: (patientId: String) -> Unit,
    showBack: Boolean = true,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val patient = uiState.patient
    val displayVitals = uiState.realtimeVitals ?: uiState.latestVitals

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.fullName ?: "My vitals") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    patient?.let {
                        IconButton(onClick = { onOpenAlerts(it.userId) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                        }
                        IconButton(onClick = { onOpenChat(it.userId) }) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null && patient == null -> Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                patient != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Patient Information", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Name", patient.fullName)
                                patient.bloodType?.let { InfoRow("Blood Type", it) }
                                patient.doctors.firstOrNull()?.let { doctor ->
                                    InfoRow("Doctor", doctor.doctorName)
                                    InfoRow("Specialization", doctor.specialization)
                                }
                            }
                        }

                        if (displayVitals?.isWearing == false) {
                            Card(Modifier.fillMaxWidth()) {
                                Text(
                                    "Watch is off-wrist — sensor readings are hidden until worn again.",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        displayVitals?.let { v ->
                            VitalsCard(
                                title = if (uiState.realtimeVitals != null) "Live Vitals" else "Latest Vitals",
                                live = uiState.realtimeVitals != null,
                                vitals = v,
                            )
                        } ?: Card(Modifier.fillMaxWidth()) {
                            Text(
                                "No vitals yet. Start monitoring on your watch.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalsCard(title: String, live: Boolean, vitals: VitalRecordDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (live) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            vitals.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm") }
            vitals.heartRateVariabilityMs?.let { VitalRow("HRV", String.format("%.1f ms", it)) }
            vitals.spO2Percent?.let { VitalRow("SpO2", "${it.toInt()}%") }
            vitals.respirationRateBpm?.let { VitalRow("Respiration", "${it.toInt()} /min") }
            vitals.temperatureC?.let { VitalRow("Body Temp", String.format("%.1f °C", it)) }
            vitals.skinTemperatureC?.let { VitalRow("Skin Temp", String.format("%.1f °C", it)) }
            if (vitals.systolicBp != null && vitals.diastolicBp != null) {
                VitalRow("Blood Pressure", "${vitals.systolicBp.toInt()}/${vitals.diastolicBp.toInt()} mmHg")
            }
            vitals.stressScore?.let { VitalRow("Stress", "${it.toInt()}/100") }
            vitals.sleepScore?.let { VitalRow("Sleep score", "${it.toInt()}/100") }
            vitals.stepsCount?.let { VitalRow("Steps", "$it") }
            vitals.caloriesBurned?.let { VitalRow("Calories", String.format("%.0f kcal", it)) }
            vitals.ecgAverageHeartRate?.let { VitalRow("ECG avg HR", "${it.toInt()} bpm") }
            vitals.ecgClassification?.let { VitalRow("ECG", it) }
            vitals.bloodGlucoseMgDl?.let { VitalRow("Glucose", String.format("%.0f mg/dL", it)) }
            vitals.bodyFatPercent?.let { VitalRow("Body Fat", String.format("%.1f %%", it)) }
            vitals.batteryLevel?.let { VitalRow("Watch battery", "${it.toInt()}%") }
            VitalRow("Watch status", if (vitals.isWearing) "On-wrist" else "Off-wrist")
            if (vitals.fallDetected) {
                Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
            }
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
