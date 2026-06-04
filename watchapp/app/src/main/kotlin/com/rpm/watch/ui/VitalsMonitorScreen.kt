package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.rpm.watch.WatchUiState
import com.rpm.watch.WatchViewModel
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.SensorType
import com.rpm.watch.service.VitalsServiceStatus
import java.util.Locale

@Composable
fun VitalsMonitorScreen(viewModel: WatchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VitalsMonitorContent(
        state = state,
        onToggle = {
            if (state.isMonitoring) viewModel.stopMonitoring()
            else viewModel.startMonitoring(state.selectedSensor)
        },
        onSensorSelected = viewModel::selectSensor,
        onPatientIdChange = viewModel::savePatientId,
    )
}

@Composable
fun VitalsMonitorContent(
    state: WatchUiState,
    onToggle: () -> Unit,
    onSensorSelected: (SensorType) -> Unit,
    onPatientIdChange: (String) -> Unit,
) {
    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.background(MaterialTheme.colors.background),
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = "ID: ${state.patientId.ifBlank { "—" }}",
                    fontSize = 8.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = mqttStatusText(state.mqttState),
                    fontSize = 8.sp,
                    color = mqttStatusColor(state.mqttState),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!state.isMonitoring && state.patientId != "ABC123") {
                item {
                    Button(
                        onClick = { onPatientIdChange("ABC123") },
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.surface,
                        ),
                    ) {
                        Text("ABC123", fontSize = 8.sp)
                    }
                }
            }
            if (state.isMonitoring) {
                item {
                    Text(
                        text = "HR · Temp · SpO₂ active",
                        fontSize = 8.sp,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Text(
                    text = "View",
                    fontSize = 8.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            item {
                SensorSelector(
                    selected = state.selectedSensor,
                    enabled = true,
                    onSelected = onSensorSelected,
                )
            }
            item {
                if (state.isMonitoring &&
                    state.selectedSensor == SensorType.HEART_RATE &&
                    state.heartRate <= 0 &&
                    state.serviceStatus == VitalsServiceStatus.CONNECTING
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        indicatorColor = MaterialTheme.colors.primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = sensorIcon(state.selectedSensor),
                            fontSize = 14.sp,
                            color = sensorColor(state),
                            modifier = Modifier.padding(end = 2.dp),
                        )
                        Text(
                            text = displayValue(state),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = sensorColor(state),
                        )
                        Text(
                            text = sensorUnit(state.selectedSensor),
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 2.dp, top = 6.dp),
                        )
                    }
                }
            }
            item {
                Text(
                    text = buildStatusText(state),
                    fontSize = 8.sp,
                    color = statusColor(state),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
            }
            item {
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (state.isMonitoring) {
                            MaterialTheme.colors.error
                        } else {
                            MaterialTheme.colors.primary
                        },
                    ),
                    modifier = Modifier.size(width = 72.dp, height = 28.dp),
                ) {
                    Text(if (state.isMonitoring) "Stop" else "Start", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SensorSelector(
    selected: SensorType,
    enabled: Boolean,
    onSelected: (SensorType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
    ) {
        SensorChip("HR", selected == SensorType.HEART_RATE, enabled) {
            onSelected(SensorType.HEART_RATE)
        }
        SensorChip("Temp", selected == SensorType.SKIN_TEMPERATURE, enabled) {
            onSelected(SensorType.SKIN_TEMPERATURE)
        }
        SensorChip("SpO₂", selected == SensorType.SPO2, enabled) {
            onSelected(SensorType.SPO2)
        }
    }
}

@Composable
private fun SensorChip(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.surface
            },
        ),
        modifier = Modifier.width(52.dp).height(24.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            maxLines = 1,
            color = if (active) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
        )
    }
}

private fun displayValue(state: WatchUiState): String = when (state.selectedSensor) {
    SensorType.HEART_RATE -> if (state.heartRate > 0) state.heartRate.toString() else "--"
    SensorType.SKIN_TEMPERATURE ->
        state.temperatureC?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
    SensorType.SPO2 ->
        state.spO2Percent?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
}

private fun sensorUnit(sensor: SensorType): String = when (sensor) {
    SensorType.HEART_RATE -> "bpm"
    SensorType.SKIN_TEMPERATURE -> "°C"
    SensorType.SPO2 -> "%"
}

private fun sensorIcon(sensor: SensorType): String = when (sensor) {
    SensorType.HEART_RATE -> "♥"
    SensorType.SKIN_TEMPERATURE -> "T"
    SensorType.SPO2 -> "O₂"
}

private fun sensorColor(state: WatchUiState): Color = when (state.selectedSensor) {
    SensorType.HEART_RATE -> heartColor(state.heartRateStatus)
    SensorType.SKIN_TEMPERATURE -> Color(0xFF1E88E5)
    SensorType.SPO2 -> Color(0xFF43A047)
}

private fun buildStatusText(state: WatchUiState): String {
    if (state.serviceStatus == VitalsServiceStatus.ERROR && state.errorMessage.isNotBlank()) {
        return state.errorMessage
    }
    val sensorStr = when (state.selectedSensor) {
        SensorType.HEART_RATE -> when {
            !state.isMonitoring -> "Tap Start"
            state.heartRate > 0 -> "Measuring ${state.heartRate} bpm"
            state.heartRateStatus == HeartRateStatus.DETACHED -> "Put watch on wrist"
            state.heartRateStatus == HeartRateStatus.MOVEMENT -> "Fasten watch snugly"
            state.heartRateStatus == HeartRateStatus.WEAK_SIGNAL -> "Hold still, screen on"
            state.heartRateStatus == HeartRateStatus.INITIAL -> "Starting heart rate…"
            state.heartRateStatus == HeartRateStatus.SENSOR_BUSY -> "Another sensor active"
            else -> "Measuring…"
        }
        SensorType.SKIN_TEMPERATURE -> if (state.temperatureC == null) {
            "Measuring temperature…"
        } else {
            "Skin temperature active"
        }
        SensorType.SPO2 -> if (state.spO2Percent == null) {
            "Measuring SpO₂…"
        } else {
            "SpO₂ active"
        }
    }
    return sensorStr
}

private fun heartColor(status: HeartRateStatus): Color = when (status) {
    HeartRateStatus.SUCCESS -> Color(0xFFE53935)
    HeartRateStatus.MOVEMENT, HeartRateStatus.WEAK_SIGNAL -> Color(0xFFFF8F00)
    HeartRateStatus.DETACHED -> Color(0xFF757575)
    else -> Color(0xFF9E9E9E)
}

private fun statusColor(state: WatchUiState): Color =
    if (state.serviceStatus == VitalsServiceStatus.ERROR) Color(0xFFFF5252) else Color(0xFFBDBDBD)

private fun mqttStatusText(state: MqttConnectionState): String = when (state) {
    MqttConnectionState.CONNECTED -> "Server: ok"
    MqttConnectionState.CONNECTING -> "Server: …"
    MqttConnectionState.ERROR -> "Server: err"
    MqttConnectionState.DISCONNECTED -> "Server: off"
}

private fun mqttStatusColor(state: MqttConnectionState): Color = when (state) {
    MqttConnectionState.CONNECTED -> Color(0xFF43A047)
    MqttConnectionState.ERROR -> Color(0xFFFF5252)
    else -> Color(0xFFBDBDBD)
}
