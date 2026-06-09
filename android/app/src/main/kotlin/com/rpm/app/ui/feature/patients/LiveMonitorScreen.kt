package com.rpm.app.ui.feature.patients

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpm.app.data.signalr.RealTimeVitals

// ──────────────────────────────────────────────────────────────────────────
// Metric tones (SupportedVitals.liveMetrics defines values)
// ──────────────────────────────────────────────────────────────────────────

private enum class MetricTone {
    Accent, Teal, Blue, Violet, Ink, Danger,
}

private data class LiveMetricCard(
    val def: SupportedVitals.MetricDef,
    val tone: MetricTone,
)

private val LIVE_METRIC_CARDS = SupportedVitals.liveMetrics.map { def ->
    val tone = when (def.label) {
        "Heart Rate"     -> MetricTone.Accent
        "SpO₂", "Watch Status" -> MetricTone.Teal
        "Skin Temp.", "Ambient Temp." -> MetricTone.Blue
        "HRV", "Stress"  -> MetricTone.Violet
        "Fall Detection" -> MetricTone.Danger
        else             -> MetricTone.Ink
    }
    LiveMetricCard(def, tone)
}

// ──────────────────────────────────────────────────────────────────────────
// Colours per tone
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricTone.containerColor(): Color = when (this) {
    MetricTone.Accent  -> MaterialTheme.colorScheme.errorContainer
    MetricTone.Teal    -> Color(0xFF003735)
    MetricTone.Blue    -> Color(0xFF003A4C)
    MetricTone.Violet  -> Color(0xFF2E004D)
    MetricTone.Ink     -> MaterialTheme.colorScheme.surfaceVariant
    MetricTone.Danger  -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun MetricTone.accentColor(): Color = when (this) {
    MetricTone.Accent  -> MaterialTheme.colorScheme.error
    MetricTone.Teal    -> Color(0xFF4DB6AC)
    MetricTone.Blue    -> Color(0xFF4FC3F7)
    MetricTone.Violet  -> Color(0xFFCE93D8)
    MetricTone.Ink     -> MaterialTheme.colorScheme.onSurfaceVariant
    MetricTone.Danger  -> MaterialTheme.colorScheme.error
}

// ──────────────────────────────────────────────────────────────────────────
// Screen
// ──────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitorScreen(
    onBack: () -> Unit,
    vm: LiveMonitorViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (state.patientName.isNotBlank()) "Live Monitor · ${state.patientName}" else "Live Monitor",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Galaxy Watch · Real-time vitals",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ConnectionStatusChip(state.connectionStatus)
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // Fall alert banner — spans both columns
            if (state.vitals?.fallDetected == true) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FallAlertBanner()
                }
            }

            // Last update row — spans both columns
            item(span = { GridItemSpan(maxLineSpan) }) {
                state.vitals?.let { v ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Icon(Icons.Default.Schedule, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Last update: ${formatVitalTimestamp(v.recordedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Supported metric cards — 2-column grid
            items(LIVE_METRIC_CARDS) { card ->
                MetricCard(vitals = state.vitals, card = card)
            }

            // History section header — spans both columns
            if (state.history.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Recent readings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Live history (last ${state.history.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // History rows — each spans both columns
                items(state.history, span = { GridItemSpan(maxLineSpan) }) { entry ->
                    HistoryRow(entry)
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(24.dp))
                }
            } else if (state.connectionStatus == ConnectionStatus.Live) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Waiting for first reading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Metric card (matches web sensor-card)
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricCard(vitals: RealTimeVitals?, card: LiveMetricCard) {
    val def = card.def
    val value = def.getValue(vitals)
    val isLive = value != "--"

    val bg by animateColorAsState(
        targetValue = if (isLive) card.tone.containerColor() else MaterialTheme.colorScheme.surface,
        animationSpec = tween(600),
        label = "cardBg",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(if (isLive) 4.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Label
            Text(
                def.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isLive) card.tone.accentColor().copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )

            // Value — big number
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize   = if (value.length > 5) 22.sp else 28.sp,
                    ),
                    color = if (isLive) card.tone.accentColor() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )

                // Unit + hint
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (def.unit.isNotEmpty()) {
                        Text(
                            def.unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLive) card.tone.accentColor().copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    def.hint?.let {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "· $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Connection status chip
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectionStatusChip(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.Live       -> "Live"       to Color(0xFF4CAF50)
        ConnectionStatus.Connecting -> "Connecting" to Color(0xFFFF9800)
        ConnectionStatus.Offline    -> "Offline"    to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue  = if (status == ConnectionStatus.Live) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (status == ConnectionStatus.Live) scale else 1f)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Fall alert banner
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun FallAlertBanner() {
    val pulse = rememberInfiniteTransition(label = "fallPulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha",
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = alpha),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("⚠ Fall Detected!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Immediate attention may be required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// History row
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryRow(entry: LiveHistoryEntry) {
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Time row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.time, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.fallDetected) {
                        HistoryBadge("Fall", MaterialTheme.colorScheme.error)
                    }
                    HistoryBadge(if (entry.wearing) "On" else "Off", if (entry.wearing) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Vitals row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryMetric("HR", "${entry.hr} bpm", Color(0xFFEF5350), Modifier.weight(1f))
                HistoryMetric("SpO₂", "${entry.spo2}%", Color(0xFF26A69A), Modifier.weight(1f))
                HistoryMetric("Skin", "${entry.skinTemp}°C", Color(0xFF42A5F5), Modifier.weight(1f))
                HistoryMetric("HRV", "${entry.hrv} ms", Color(0xFFAB47BC), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryMetric("Amb.", "${entry.ambientTemp}°C", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                HistoryMetric("Stress", entry.stress, Color(0xFFAB47BC), Modifier.weight(1f))
                HistoryMetric("Steps", entry.steps, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace), textAlign = TextAlign.Center)
    }
}

@Composable
private fun HistoryBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
