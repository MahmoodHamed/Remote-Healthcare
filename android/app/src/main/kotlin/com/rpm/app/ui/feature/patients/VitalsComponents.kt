package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.remote.dto.VitalRecordLatestDto
import com.rpm.app.data.signalr.RealTimeVitals
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// ── Section title ──────────────────────────────────────────────────────────

fun vitalsSectionTitle(role: String?, subjectName: String?): String = when (role) {
    "Patient"  -> "My Vitals"
    "Doctor"   -> subjectName?.let { "Vitals — $it" } ?: "Patient Vitals"
    "Relative" -> subjectName?.let { "Vitals — $it" } ?: "Family Member Vitals"
    else       -> "Vitals"
}

// ── Summary text for list cards ────────────────────────────────────────────

@Composable
fun VitalsSummaryText(vitals: VitalRecordLatestDto, modifier: Modifier = Modifier) {
    Text(
        buildVitalsSummary(vitals),
        style    = MaterialTheme.typography.bodySmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

fun buildVitalsSummary(v: VitalRecordLatestDto): String = SupportedVitals.buildSummary(v)

// ── Detail card dispatcher ─────────────────────────────────────────────────

@Composable
fun VitalsDetailCard(
    title:    String,
    latest:   VitalRecordLatestDto? = null,
    full:     VitalRecordDto?       = null,
    live:     RealTimeVitals?       = null,
    modifier: Modifier              = Modifier,
) {
    when {
        live   != null -> LiveVitalsCard(title, live, modifier)
        latest != null -> StaticVitalsCard(title, latest, modifier)
        full   != null -> FullVitalsCard(title, full, modifier)
        else           -> EmptyVitalsCard(title, modifier)
    }
}

// ── Empty ──────────────────────────────────────────────────────────────────

@Composable
private fun EmptyVitalsCard(title: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            VitalsCardHeader(title = title, live = false)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WatchOff,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "No vitals recorded yet. Data will appear when the wearable syncs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Static (last snapshot) ─────────────────────────────────────────────────

@Composable
private fun StaticVitalsCard(title: String, v: VitalRecordLatestDto, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            VitalsCardHeader(title = title, live = false)
            v.recordedAt?.let {
                Text(
                    "Updated: ${formatVitalTimestamp(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            renderLatestRows(v)
        }
    }
}

// ── Full VitalRecordDto ────────────────────────────────────────────────────

@Composable
private fun FullVitalsCard(title: String, v: VitalRecordDto, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            VitalsCardHeader(title = title, live = false)
            Text(
                "Updated: ${formatVitalTimestamp(v.recordedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            renderSupportedRows(v)
            if (v.fallDetected) {
                Spacer(Modifier.height(4.dp))
                FallAlert()
            }
        }
    }
}

// ── Live ───────────────────────────────────────────────────────────────────

@Composable
private fun LiveVitalsCard(title: String, rv: RealTimeVitals, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            VitalsCardHeader(title = title, live = true)
            Spacer(Modifier.height(12.dp))

            renderLiveRows(rv)
            if (rv.fallDetected) {
                Spacer(Modifier.height(4.dp))
                FallAlert()
            }
        }
    }
}

// ── Shared row rendering ───────────────────────────────────────────────────

@Composable
private fun ColumnScope.renderLatestRows(v: VitalRecordLatestDto) {
    v.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm", statusFor(it, 40f..100f)) }
    v.spO2Percent?.let  { VitalRow("SpO₂", "${it.toInt()}%", statusFor(it, 95f..100f)) }
    v.skinTemperatureC?.let { VitalRow("Skin Temp. (Wrist)", "%.1f °C".format(it), normalIcon = true) }
    (v.ambientTemperatureC ?: v.temperatureC)?.let {
        VitalRow("Ambient Temp. (Room)", "%.1f °C".format(it), normalIcon = true)
    }
    v.hrvMs?.let       { VitalRow("HRV", "${it.toInt()} ms", normalIcon = true) }
    v.stressScore?.let { VitalRow("Stress", "${it.toInt()} / 100", stressStatus(it)) }
}

@Composable
private fun ColumnScope.renderSupportedRows(v: VitalRecordDto) {
    v.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm", statusFor(it, 40f..100f)) }
    v.spO2Percent?.let  { VitalRow("SpO₂", "${it.toInt()}%", statusFor(it, 95f..100f)) }
    v.skinTemperatureC?.let { VitalRow("Skin Temp. (Wrist)", "%.1f °C".format(it), normalIcon = true) }
    // temperatureC stores ambient/room temperature (mapped from ambientTemperatureC in MQTT)
    (v.ambientTemperatureC ?: v.temperatureC)?.let {
        VitalRow("Ambient Temp. (Room)", "%.1f °C".format(it), normalIcon = true)
    }
    v.hrvMs?.let       { VitalRow("HRV", "${it.toInt()} ms", normalIcon = true) }
    v.stressScore?.let { VitalRow("Stress", "${it.toInt()} / 100", stressStatus(it)) }
    v.stepsCount?.let        { VitalRow("Steps", "$it", normalIcon = true) }
    v.caloriesBurned?.let    { VitalRow("Calories", "%.0f kcal".format(it), normalIcon = true) }
    v.bodyFatPercent?.let    { VitalRow("Body Fat", "%.1f%%".format(it), normalIcon = true) }
    v.ecgAvgHeartRateBpm?.let { VitalRow("ECG Avg HR", "${it.toInt()} bpm", normalIcon = true) }
    VitalRow("Watch", if (SupportedVitals.isWearing(v)) "On wrist ✓" else "Off wrist", normalIcon = SupportedVitals.isWearing(v))
}

@Composable
private fun ColumnScope.renderLiveRows(rv: RealTimeVitals) {
    rv.heartRateBpm?.let { VitalRow("Heart Rate", "${it.toInt()} bpm", statusFor(it, 40f..100f)) }
    rv.spO2Percent?.let   { VitalRow("SpO₂", "${it.toInt()}%", statusFor(it, 95f..100f)) }
    // After withNormalizedTemperatures(): skinTemperatureC = wrist, ambientTemperatureC = room, temperatureC = null
    rv.skinTemperatureC?.let    { VitalRow("Skin Temp. (Wrist)", "%.1f °C".format(it), normalIcon = true) }
    rv.ambientTemperatureC?.let { VitalRow("Ambient Temp. (Room)", "%.1f °C".format(it), normalIcon = true) }
    rv.hrvMs?.let         { VitalRow("HRV", "${it.toInt()} ms", normalIcon = true) }
    rv.stressScore?.let   { VitalRow("Stress", "${it.toInt()} / 100", stressStatus(it)) }
    rv.bodyFatPercent?.let { VitalRow("Body Fat", "%.1f%%".format(it), normalIcon = true) }
    rv.ecgAvgHeartRateBpm?.let { VitalRow("ECG Avg HR", "${it.toInt()} bpm", normalIcon = true) }
    rv.stepsCount?.let    { VitalRow("Steps", "$it", normalIcon = true) }
    rv.caloriesBurned?.let { VitalRow("Calories", "%.0f kcal".format(it), normalIcon = true) }
    VitalRow("Watch", if (SupportedVitals.isWearing(rv)) "On wrist ✓" else "Off wrist", normalIcon = SupportedVitals.isWearing(rv))
}

// ── Vital Row ──────────────────────────────────────────────────────────────

@Composable
fun VitalRow(
    label:      String,
    value:      String,
    status:     VitalStatus = VitalStatus.Normal,
    normalIcon: Boolean     = false,
) {
    val dotColor = when (status) {
        VitalStatus.Normal   -> Color(0xFF2E7D32)
        VitalStatus.Warning  -> Color(0xFFF57C00)
        VitalStatus.Critical -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint     = if (normalIcon) dotColor else dotColor,
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

// ── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun VitalsCardHeader(title: String, live: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (live) Icons.Default.FiberManualRecord else Icons.Default.Favorite,
            contentDescription = null,
            tint     = if (live) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (live) 12.dp else 18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (live) "$title  •  Live" else title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun FallAlert() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Fall Detected!", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ── Status helpers ─────────────────────────────────────────────────────────

enum class VitalStatus { Normal, Warning, Critical }

private fun statusFor(value: Float, normalRange: ClosedFloatingPointRange<Float>): VitalStatus = when {
    value in normalRange -> VitalStatus.Normal
    else                 -> VitalStatus.Warning
}

private fun stressStatus(score: Float) = when {
    score < 40f  -> VitalStatus.Normal
    score < 70f  -> VitalStatus.Warning
    else         -> VitalStatus.Critical
}

// ── Timestamp ─────────────────────────────────────────────────────────────

fun formatVitalTimestamp(isoString: String): String = runCatching {
    val instant = Instant.parse(isoString)
    val local   = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val today   = LocalDateTime.now(ZoneId.systemDefault()).toLocalDate()
    val date    = local.toLocalDate()
    val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    when (date) {
        today                  -> "Today ${local.format(timeFmt)}"
        today.minusDays(1)     -> "Yesterday ${local.format(timeFmt)}"
        else                   -> {
            val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            "${local.format(dateFmt)} ${local.format(timeFmt)}"
        }
    }
}.getOrDefault(isoString)
