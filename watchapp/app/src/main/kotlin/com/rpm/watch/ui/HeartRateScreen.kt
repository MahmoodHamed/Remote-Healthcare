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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.rpm.watch.WatchUiState
import com.rpm.watch.WatchViewModel
import com.rpm.watch.health.HrStatus
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.service.ServiceStatus

@Composable
fun HeartRateScreen(viewModel: WatchViewModel, onOpenSettings: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HeartRateContent(
        state = state,
        onToggle = {
            if (state.isMonitoring) viewModel.stopMonitoring()
            else viewModel.startMonitoring()
        },
        onOpenSettings = onOpenSettings,
        onMeasureSpO2 = viewModel::measureSpO2,
        onMeasureEcg = viewModel::measureEcg,
        onMeasureBodyFat = viewModel::measureBodyFat,
    )
}

@Composable
fun HeartRateContent(
    state: WatchUiState,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onMeasureSpO2: () -> Unit = {},
    onMeasureEcg: () -> Unit = {},
    onMeasureBodyFat: () -> Unit = {},
) {
    val scroll = rememberScrollState()
    Scaffold(
        timeText  = { TimeText() },
        modifier  = Modifier.background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.serviceStatus == ServiceStatus.CONNECTING || state.isMeasuringOnDemand) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    indicatorColor = MaterialTheme.colors.primary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "❤", fontSize = 18.sp, color = heartColor(state.hrStatus))
                Text(
                    text = if (state.heartRate > 0) "${state.heartRate}" else "--",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = heartColor(state.hrStatus),
                )
                Text(text = " bpm", fontSize = 12.sp, color = Color(0xFFBDBDBD))
            }

            Text(
                text = buildStatusText(state),
                fontSize = 10.sp,
                color = statusColor(state),
                textAlign = TextAlign.Center
            )

            if (state.patientId.isNotBlank()) {
                Text(text = "ID: ${state.patientId}", fontSize = 9.sp, color = Color(0xFF757575))
            }

            Spacer(Modifier.height(6.dp))
            SensorRow("Heart rate", fmt(state.sensors.heartRateBpm))
            SensorRow("HRV", fmt(state.sensors.heartRateVariabilityMs, " ms"))
            SensorRow("SpO₂", fmt(state.sensors.spO2Percent, " %"))
            SensorRow("Skin temp.", fmt(state.sensors.skinTemperatureC, " °C"))
            SensorRow("Ambient temp.", fmt(state.sensors.ambientTemperatureC, " °C"))
            SensorRow("Stress", fmt(state.sensors.stressScore))
            SensorRow("Steps", state.sensors.stepsCount?.toString() ?: "--")
            SensorRow("ECG avg HR", fmt(state.sensors.ecgAverageHeartRate))
            SensorRow("Body fat", fmt(state.sensors.bodyFatPercent, " %"))
            SensorRow("Watch status", if (state.isMonitoring) "On" else "Off")

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onMeasureBodyFat,
                    enabled = state.isMonitoring && !state.isMeasuringOnDemand,
                    modifier = Modifier.size(width = 72.dp, height = 32.dp),
                ) { Text("Body Fat", fontSize = 9.sp) }
                Button(
                    onClick = onMeasureEcg,
                    enabled = state.isMonitoring && !state.isMeasuringOnDemand,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    modifier = Modifier.size(width = 72.dp, height = 32.dp),
                ) { Text("ECG", fontSize = 9.sp) }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (state.isMonitoring) MaterialTheme.colors.error else MaterialTheme.colors.primary
                ),
                modifier = Modifier.size(width = 100.dp, height = 34.dp)
            ) {
                Text(if (state.isMonitoring) "Stop" else "Start", fontSize = 12.sp)
            }

            if (state.measureMessage.isNotBlank()) {
                Text(
                    text = state.measureMessage,
                    fontSize = 9.sp,
                    color = Color(0xFF81C784),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(onClick = onOpenSettings, modifier = Modifier.size(width = 80.dp, height = 28.dp)) {
                Text("Setup", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SensorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF9E9E9E))
        Text(text = value, fontSize = 10.sp, color = Color.White)
    }
}

private fun fmt(value: Float?, suffix: String = ""): String =
    if (value == null) "--" else "${if (value % 1f == 0f) value.toInt() else "%.1f".format(value)}$suffix"

private fun buildStatusText(state: WatchUiState): String {
    if (state.serviceStatus == ServiceStatus.ERROR && state.errorMessage.isNotBlank()) {
        return state.errorMessage
    }
    val serverStr = when (state.mqttConnectionState) {
        MqttConnectionState.CONNECTED    -> "Shared with server"
        MqttConnectionState.CONNECTING   -> "Connecting to server…"
        MqttConnectionState.ERROR        -> "Server error"
        MqttConnectionState.DISCONNECTED -> "No server"
    }
    return if (state.isMonitoring) serverStr else "Tap Start to monitor"
}

private fun heartColor(status: HrStatus): Color = when (status) {
    HrStatus.GOOD         -> Color(0xFFE53935)
    HrStatus.MOVING       -> Color(0xFFFF8F00)
    else                  -> Color(0xFF9E9E9E)
}

private fun statusColor(state: WatchUiState): Color =
    if (state.serviceStatus == ServiceStatus.ERROR) Color(0xFFFF5252) else Color(0xFFBDBDBD)
