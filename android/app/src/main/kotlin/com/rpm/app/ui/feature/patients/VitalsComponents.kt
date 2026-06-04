package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.remote.dto.VitalRecordLatestDto
import com.rpm.app.data.signalr.RealTimeVitals

fun vitalsSectionTitle(role: String?, subjectName: String?): String = when (role) {
    "Patient" -> "My Vitals"
    "Doctor" -> subjectName?.let { "Vitals — $it" } ?: "Patient Vitals"
    "Relative" -> subjectName?.let { "Vitals — $it" } ?: "Family Member Vitals"
    else -> "Vitals"
}

@Composable
fun VitalsSummaryText(vitals: VitalRecordLatestDto, modifier: Modifier = Modifier) {
    Text(
        buildVitalsSummary(vitals),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

fun buildVitalsSummary(v: VitalRecordLatestDto): String = buildString {
    v.heartRateBpm?.let { append("HR: ${it.toInt()} bpm") }
    v.spO2Percent?.let {
        if (isNotEmpty()) append("  ")
        append("SpO₂: ${it.toInt()}%")
    }
    v.temperatureC?.let {
        if (isNotEmpty()) append("  ")
        append(String.format("%.1f °C", it))
    }
    if (v.systolicBp != null && v.diastolicBp != null) {
        if (isNotEmpty()) append("  ")
        append("BP: ${v.systolicBp.toInt()}/${v.diastolicBp.toInt()}")
    }
    if (isEmpty()) append("No vitals yet")
}

@Composable
fun VitalsDetailCard(
    title: String,
    latest: VitalRecordLatestDto?,
    full: VitalRecordDto? = null,
    live: RealTimeVitals? = null,
    modifier: Modifier = Modifier,
) {
    when {
        live != null -> LiveVitalsCard(title, live, modifier)
        latest != null -> StaticVitalsCard(title, latest, modifier)
        full != null -> FullVitalsCard(title, full, modifier)
        else -> EmptyVitalsCard(title, modifier)
    }
}

@Composable
private fun EmptyVitalsCard(title: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "No vitals recorded yet. Data will appear when the wearable syncs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StaticVitalsCard(title: String, v: VitalRecordLatestDto, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            v.recordedAt?.let {
                Spacer(Modifier.height(4.dp))
                Text("Last updated: $it", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            renderLatestRows(v)
        }
    }
}

@Composable
private fun FullVitalsCard(title: String, v: VitalRecordDto, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Last updated: ${v.recordedAt}", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            renderLatestRows(
                VitalRecordLatestDto(
                    heartRateBpm = v.heartRateBpm,
                    spO2Percent = v.spO2Percent,
                    systolicBp = v.systolicBp,
                    diastolicBp = v.diastolicBp,
                    temperatureC = v.temperatureC,
                    recordedAt = v.recordedAt,
                )
            )
            VitalRow("Wearing Watch", if (v.isWearing) "Yes" else "No")
            if (v.fallDetected) {
                Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LiveVitalsCard(title: String, rv: RealTimeVitals, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("$title (Live)", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            rv.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm") }
            rv.spO2Percent?.let { VitalRow("SpO₂", "${it.toInt()}%") }
            rv.temperatureC?.let { VitalRow("Temperature", String.format("%.1f °C", it)) }
            if (rv.systolicBp != null && rv.diastolicBp != null) {
                VitalRow("Blood Pressure", "${rv.systolicBp.toInt()}/${rv.diastolicBp.toInt()} mmHg")
            }
            if (rv.fallDetected) {
                Text("⚠ Fall Detected!", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ColumnScope.renderLatestRows(v: VitalRecordLatestDto) {
    v.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm") }
    v.spO2Percent?.let { VitalRow("SpO₂", "${it.toInt()}%") }
    v.temperatureC?.let { VitalRow("Temperature", String.format("%.1f °C", it)) }
    if (v.systolicBp != null && v.diastolicBp != null) {
        VitalRow("Blood Pressure", "${v.systolicBp.toInt()}/${v.diastolicBp.toInt()} mmHg")
    }
}

@Composable
fun VitalRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
