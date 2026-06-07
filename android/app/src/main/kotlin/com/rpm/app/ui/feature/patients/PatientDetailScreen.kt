package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.util.ShortIdNormalizer

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

    var editingShortId by remember { mutableStateOf(false) }
    var shortIdInput by remember(uiState.watchShortId) { mutableStateOf(uiState.watchShortId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.fullName ?: "My vitals") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    patient?.let {
                        IconButton(onClick = { onOpenAlerts(it.userId) }) {
                            Icon(Icons.Default.Notifications, "Alerts")
                        }
                        IconButton(onClick = { onOpenChat(it.userId) }) {
                            Icon(Icons.Default.Chat, "Chat")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Watch pairing card ──────────────────────────────────
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Watch, contentDescription = null,
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Watch pairing", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(8.dp))

                            if (editingShortId) {
                                OutlinedTextField(
                                    value = shortIdInput,
                                    onValueChange = { shortIdInput = it.uppercase().take(6) },
                                    label = { Text("6-char short ID (e.g. ABC123)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        keyboardType = KeyboardType.Ascii,
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                    isError = shortIdInput.isNotEmpty() && !ShortIdNormalizer.isValidShortId(shortIdInput),
                                    supportingText = {
                                        if (shortIdInput.isNotEmpty() && !ShortIdNormalizer.isValidShortId(shortIdInput))
                                            Text("Must be exactly 6 letters or digits")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        shortIdInput = uiState.watchShortId
                                        editingShortId = false
                                    }) { Text("Cancel") }
                                    Button(
                                        onClick = {
                                            viewModel.saveWatchShortId(shortIdInput)
                                            editingShortId = false
                                        },
                                        enabled = ShortIdNormalizer.isValidShortId(shortIdInput)
                                    ) { Text("Save") }
                                }
                            } else {
                                if (uiState.watchShortId.isNotEmpty()) {
                                    InfoRow("Short ID", uiState.watchShortId)
                                    InfoRow(
                                        "Streaming ID",
                                        uiState.streamingPatientId.take(18) + "…"
                                    )
                                } else {
                                    Text(
                                        "Enter the 6-char ID shown on your watch app → Setup screen. This must match the ID on the watch.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { editingShortId = true }) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (uiState.watchShortId.isEmpty()) "Set watch ID" else "Change ID")
                                }
                            }
                        }
                    }

                    // ── No ID warning ───────────────────────────────────────
                    if (uiState.watchShortId.isEmpty()) {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "⚠ Set your watch short ID above to see live vitals.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    // ── Error ───────────────────────────────────────────────
                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    // ── Off-wrist notice ────────────────────────────────────
                    if (displayVitals?.isWearing == false) {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "⌚ Watch is off-wrist — readings hidden until worn again.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // ── Vitals ──────────────────────────────────────────────
                    if (displayVitals != null) {
                        VitalsCard(
                            title = if (uiState.realtimeVitals != null) "Live Vitals" else "Latest Vitals",
                            live = uiState.realtimeVitals != null,
                            vitals = displayVitals,
                        )
                    } else if (uiState.watchShortId.isNotEmpty() && !uiState.isLoading) {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "No vitals yet. Start monitoring on your watch.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    // ── Patient info ────────────────────────────────────────
                    patient?.let { p ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Patient information", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Name", p.fullName)
                                p.bloodType?.let { InfoRow("Blood Type", it) }
                                p.doctors.firstOrNull()?.let { doctor ->
                                    InfoRow("Doctor", doctor.doctorName)
                                    InfoRow("Specialization", doctor.specialization)
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
private fun VitalsCard(title: String, live: Boolean, vitals: VitalRecordDto) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (live) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    Icon(Icons.Default.FiberManualRecord, null,
                        tint = Color.Green, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            vitals.heartRateBpm?.let       { VitalRow("Heart Rate",    "${it.toInt()} bpm") }
            vitals.heartRateVariabilityMs?.let { VitalRow("HRV",       String.format("%.1f ms", it)) }
            vitals.spO2Percent?.let        { VitalRow("SpO₂",          "${it.toInt()}%") }
            vitals.respirationRateBpm?.let { VitalRow("Respiration",   "${it.toInt()} /min") }
            vitals.temperatureC?.let       { VitalRow("Body Temp",     String.format("%.1f °C", it)) }
            vitals.skinTemperatureC?.let   { VitalRow("Skin Temp",     String.format("%.1f °C", it)) }
            if (vitals.systolicBp != null && vitals.diastolicBp != null)
                VitalRow("Blood Pressure", "${vitals.systolicBp.toInt()}/${vitals.diastolicBp.toInt()} mmHg")
            vitals.stressScore?.let        { VitalRow("Stress",        "${it.toInt()}/100") }
            vitals.sleepScore?.let         { VitalRow("Sleep score",   "${it.toInt()}/100") }
            vitals.stepsCount?.let         { VitalRow("Steps",         "$it") }
            vitals.caloriesBurned?.let     { VitalRow("Calories",      String.format("%.0f kcal", it)) }
            vitals.ecgAverageHeartRate?.let { VitalRow("ECG avg HR",   "${it.toInt()} bpm") }
            vitals.ecgClassification?.let  { VitalRow("ECG",           it) }
            vitals.bloodGlucoseMgDl?.let   { VitalRow("Glucose",      String.format("%.0f mg/dL", it)) }
            vitals.bodyFatPercent?.let     { VitalRow("Body Fat",      String.format("%.1f %%", it)) }
            vitals.batteryLevel?.let       { VitalRow("Watch battery", "${it.toInt()}%") }
            VitalRow("Watch status", if (vitals.isWearing) "On-wrist" else "Off-wrist")
            if (vitals.fallDetected)
                Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun VitalRow(label: String, value: String) = InfoRow(label, value)
