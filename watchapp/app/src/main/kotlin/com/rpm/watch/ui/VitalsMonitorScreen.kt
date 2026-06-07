package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.rpm.watch.service.VitalsServiceStatus

@Composable
fun VitalsMonitorScreen(
    viewModel: WatchViewModel,
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VitalsMonitorContent(
        state = state,
        onToggle = {
            if (state.isMonitoring) viewModel.stopMonitoring()
            else viewModel.startMonitoring()
        },
        onMetricSelected = viewModel::selectMetric,
        onOpenSettings = onOpenSettings,
        onMeasureEcg = viewModel::startEcgMeasurement,
        onMeasureBodyFat = viewModel::startBodyFatMeasurement,
        onOpenSamsungHealth = viewModel::openSamsungHealthMonitor,
    )
}

@Composable
fun VitalsMonitorContent(
    state: WatchUiState,
    onToggle: () -> Unit,
    onMetricSelected: (WatchViewMetric) -> Unit,
    onOpenSettings: () -> Unit,
    onMeasureEcg: () -> Unit,
    onMeasureBodyFat: () -> Unit,
    onOpenSamsungHealth: () -> Unit,
) {
    val metric = state.selectedMetric

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
            item {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(22.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
                ) {
                    Text("Settings", fontSize = 8.sp)
                }
            }
            if (state.isMonitoring) {
                item {
                    Text(
                        text = "Sharing: HR · Skin · SpO₂ · Stress · Steps",
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
                MetricSelector(
                    selected = metric,
                    onSelected = onMetricSelected,
                )
            }
            item {
                if (state.isMonitoring &&
                    metric == WatchViewMetric.HEART_RATE &&
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
                            text = metricIcon(metric),
                            fontSize = 14.sp,
                            color = metricColor(metric, state),
                            modifier = Modifier.padding(end = 2.dp),
                        )
                        Text(
                            text = SupportedWatchVitals.formatValue(metric, state),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = metricColor(metric, state),
                        )
                        if (metric.unit.isNotEmpty()) {
                            Text(
                                text = metric.unit,
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 2.dp, top = 6.dp),
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = SupportedWatchVitals.statusText(metric, state),
                    fontSize = 8.sp,
                    color = statusColor(state),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
            }
            if (state.isMonitoring) {
                item {
                    Text(
                        "Shared with server",
                        fontSize = 8.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
                SupportedWatchVitals.sharingSummary(state).forEach { (label, value) ->
                    item(key = label) {
                        SharedMetricRow(label, value)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = onMeasureBodyFat,
                            enabled = !state.biaMeasuring,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF455A64)),
                            modifier = Modifier.weight(1f).height(26.dp),
                        ) {
                            Text(if (state.biaMeasuring) "BIA…" else "Body Fat", fontSize = 8.sp)
                        }
                        Button(
                            onClick = onMeasureEcg,
                            enabled = !state.ecgMeasuring,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5E35B1)),
                            modifier = Modifier.weight(1f).height(26.dp),
                        ) {
                            Text(if (state.ecgMeasuring) "ECG…" else "ECG", fontSize = 8.sp)
                        }
                    }
                }
                if (state.bodyFatPercent != null) {
                    item {
                        Text(
                            text = "Body fat: ${SupportedWatchVitals.formatValue(WatchViewMetric.BODY_FAT, state)}% → sent",
                            fontSize = 8.sp,
                            color = Color(0xFF43A047),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (state.ecgAvgHeartRateBpm != null) {
                    item {
                        Text(
                            text = "ECG avg HR: ${state.ecgAvgHeartRateBpm.toInt()} bpm → sent",
                            fontSize = 8.sp,
                            color = Color(0xFF43A047),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (state.samsungHealthMonitorInstalled) {
                    item {
                        Button(
                            onClick = onOpenSamsungHealth,
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
                            modifier = Modifier.fillMaxWidth().height(22.dp),
                        ) {
                            Text("Samsung Health", fontSize = 8.sp)
                        }
                    }
                }
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
private fun SharedMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 7.sp, color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f))
        Text(value, fontSize = 7.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricSelector(
    selected: WatchViewMetric,
    onSelected: (WatchViewMetric) -> Unit,
) {
    val rows = SupportedWatchVitals.viewableMetrics.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            ) {
                row.forEach { metric ->
                    MetricChip(
                        label = metric.chipLabel,
                        active = selected == metric,
                        onClick = { onSelected(metric) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        ),
        modifier = Modifier.width(40.dp).height(22.dp),
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            maxLines = 1,
            color = if (active) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
        )
    }
}

private fun metricIcon(metric: WatchViewMetric): String = when (metric) {
    WatchViewMetric.HEART_RATE -> "♥"
    WatchViewMetric.HRV -> "∿"
    WatchViewMetric.SPO2 -> "O₂"
    WatchViewMetric.SKIN_TEMP, WatchViewMetric.AMBIENT_TEMP -> "T"
    WatchViewMetric.STRESS -> "S"
    WatchViewMetric.STEPS -> "👣"
    WatchViewMetric.CALORIES -> "C"
    WatchViewMetric.FALL -> "!"
    WatchViewMetric.WEAR -> "⌚"
    WatchViewMetric.BODY_FAT -> "B"
    WatchViewMetric.ECG -> "E"
}

private fun metricColor(metric: WatchViewMetric, state: WatchUiState): Color = when (metric) {
    WatchViewMetric.HEART_RATE -> heartColor(state.heartRateStatus)
    WatchViewMetric.SPO2 -> Color(0xFF43A047)
    WatchViewMetric.SKIN_TEMP -> Color(0xFF1E88E5)
    WatchViewMetric.AMBIENT_TEMP -> Color(0xFF546E7A)
    WatchViewMetric.STRESS, WatchViewMetric.HRV -> Color(0xFF8E24AA)
    WatchViewMetric.FALL -> if (state.fallDetected) Color(0xFFE53935) else Color(0xFF757575)
    WatchViewMetric.WEAR -> if (state.isWearing) Color(0xFF43A047) else Color(0xFF757575)
    else -> Color(0xFF757575)
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
