package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.rpm.watch.MonitoringMode
import com.rpm.watch.WatchUiState
import com.rpm.watch.WatchViewModel
import com.rpm.watch.health.HrStatus
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.service.ServiceStatus
import java.util.Locale

@Composable
fun HeartRateScreen(viewModel: WatchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HeartRateContent(state = state, onToggle = {
        if (state.isMonitoring) viewModel.stopMonitoring()
        else viewModel.startMonitoring(state.selectedMode)
    }, onModeSelected = viewModel::selectMode, onPatientIdChange = viewModel::savePatientId)
}

@Composable
fun HeartRateContent(
    state: WatchUiState,
    onToggle: () -> Unit,
    onModeSelected: (MonitoringMode) -> Unit,
    onPatientIdChange: (String) -> Unit
) {
    Scaffold(
        timeText  = { TimeText() },
        modifier  = Modifier.background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Patient ID: ${state.patientId.ifBlank { "not set" }}",
                fontSize = 10.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                text = mqttStatusText(state.mqttState),
                fontSize = 10.sp,
                color = mqttStatusColor(state.mqttState),
                textAlign = TextAlign.Center
            )
            if (!state.isMonitoring && state.patientId != "ABC123") {
                Button(
                    onClick = { onPatientIdChange("ABC123") },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    Text("Use ABC123", fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Choose sensor",
                fontSize = 11.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            ModeSelector(
                selectedMode = state.selectedMode,
                enabled = !state.isMonitoring,
                onModeSelected = onModeSelected
            )

            Spacer(Modifier.height(10.dp))

            // ── Live value ────────────────────────────────────────────────────
            if (state.serviceStatus == ServiceStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier         = Modifier.size(40.dp),
                    indicatorColor   = MaterialTheme.colors.primary,
                    strokeWidth      = 3.dp
                )
            } else {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text       = modeIcon(state.selectedMode),
                        fontSize   = 22.sp,
                        color      = modeColor(state),
                        modifier   = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text       = displayValue(state),
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color      = modeColor(state),
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text     = " ${modeUnit(state.selectedMode)}",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Status line ───────────────────────────────────────────────────
            Text(
                text     = buildStatusText(state),
                fontSize = 11.sp,
                color    = statusColor(state),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // ── Toggle button ─────────────────────────────────────────────────
            Button(
                onClick = onToggle,
                colors  = ButtonDefaults.buttonColors(
                    backgroundColor = if (state.isMonitoring)
                        MaterialTheme.colors.error
                    else
                        MaterialTheme.colors.primary
                ),
                modifier = Modifier.size(width = 90.dp, height = 36.dp)
            ) {
                Text(
                    text     = if (state.isMonitoring) "Stop" else "Start",
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    selectedMode: MonitoringMode,
    enabled: Boolean,
    onModeSelected: (MonitoringMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ModeButton(
            label = "Heart Rate",
            active = selectedMode == MonitoringMode.HEART_RATE,
            enabled = enabled,
            onClick = { onModeSelected(MonitoringMode.HEART_RATE) }
        )
        ModeButton(
            label = "Temperature",
            active = selectedMode == MonitoringMode.TEMPERATURE,
            enabled = enabled,
            onClick = { onModeSelected(MonitoringMode.TEMPERATURE) }
        )
        ModeButton(
            label = "SpO2",
            active = selectedMode == MonitoringMode.SPO2,
            enabled = enabled,
            onClick = { onModeSelected(MonitoringMode.SPO2) }
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.surface
        ),
        modifier = Modifier.fillMaxWidth().height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (active) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
        )
    }
}

private fun displayValue(state: WatchUiState): String = when (state.selectedMode) {
    MonitoringMode.HEART_RATE -> if (state.heartRate > 0) state.heartRate.toString() else "--"
    MonitoringMode.TEMPERATURE -> state.temperatureC?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
    MonitoringMode.SPO2 -> state.spO2Percent?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
}

private fun modeUnit(mode: MonitoringMode): String = when (mode) {
    MonitoringMode.HEART_RATE -> "bpm"
    MonitoringMode.TEMPERATURE -> "°C"
    MonitoringMode.SPO2 -> "%"
}

private fun modeIcon(mode: MonitoringMode): String = when (mode) {
    MonitoringMode.HEART_RATE -> "❤"
    MonitoringMode.TEMPERATURE -> "T"
    MonitoringMode.SPO2 -> "O2"
}

private fun modeColor(state: WatchUiState): Color = when (state.selectedMode) {
    MonitoringMode.HEART_RATE -> heartColor(state.hrStatus)
    MonitoringMode.TEMPERATURE -> Color(0xFF1E88E5)
    MonitoringMode.SPO2 -> Color(0xFF43A047)
}

private fun buildStatusText(state: WatchUiState): String {
    if (state.serviceStatus == ServiceStatus.ERROR && state.errorMessage.isNotBlank()) {
        return state.errorMessage
    }

    val sensorStr = when (state.selectedMode) {
        MonitoringMode.HEART_RATE -> when (state.hrStatus) {
            HrStatus.GOOD         -> "Good"
            HrStatus.MOVING       -> "Moving"
            HrStatus.DEVICE_MOVING -> "Still"
            HrStatus.LOW_PASS     -> "Low"
            HrStatus.INITIAL      -> "Place watch firmly"
        }
        MonitoringMode.TEMPERATURE -> if (state.temperatureC == null) "Waiting for temperature" else "Temperature active"
        MonitoringMode.SPO2 -> if (state.spO2Percent == null) "Waiting for SpO2" else "SpO2 active"
    }
    return if (state.isMonitoring) sensorStr else "Tap Start to monitor"
}

private fun heartColor(status: HrStatus): Color = when (status) {
    HrStatus.GOOD         -> Color(0xFFE53935)   // red
    HrStatus.MOVING       -> Color(0xFFFF8F00)   // amber
    else                  -> Color(0xFF9E9E9E)   // grey
}

private fun statusColor(state: WatchUiState): Color =
    if (state.serviceStatus == ServiceStatus.ERROR) Color(0xFFFF5252) else Color(0xFFBDBDBD)

private fun mqttStatusText(state: MqttConnectionState): String = when (state) {
    MqttConnectionState.CONNECTED -> "Server: connected"
    MqttConnectionState.CONNECTING -> "Server: connecting…"
    MqttConnectionState.ERROR -> "Server: error"
    MqttConnectionState.DISCONNECTED -> "Server: offline"
}

private fun mqttStatusColor(state: MqttConnectionState): Color = when (state) {
    MqttConnectionState.CONNECTED -> Color(0xFF43A047)
    MqttConnectionState.ERROR -> Color(0xFFFF5252)
    else -> Color(0xFFBDBDBD)
}
