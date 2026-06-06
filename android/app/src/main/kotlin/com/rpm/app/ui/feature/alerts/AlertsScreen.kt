package com.rpm.app.ui.feature.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.AlertDto
import com.rpm.app.ui.feature.patients.formatVitalTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show snackbar on action errors
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alerts")
                        viewModel.patientId?.let {
                            Text(
                                "Patient alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadAlerts) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Filter Tabs ───────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = AlertFilter.entries.indexOf(uiState.filter),
                edgePadding      = 12.dp,
                divider          = {},
                modifier         = Modifier.fillMaxWidth(),
            ) {
                AlertFilter.entries.forEach { filter ->
                    val count = if (filter == AlertFilter.All) uiState.alerts.size
                                else uiState.alerts.count { it.status.equals(filter.label, ignoreCase = true) }
                    Tab(
                        selected = uiState.filter == filter,
                        onClick  = { viewModel.setFilter(filter) },
                        text     = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(filter.label)
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge(
                                        containerColor = if (uiState.filter == filter)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(count.toString())
                                    }
                                }
                            }
                        },
                    )
                }
            }

            HorizontalDivider()

            // ── Content ───────────────────────────────────────────────────
            Box(Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    uiState.error != null -> Column(
                        modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            uiState.error!!,
                            color     = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(onClick = viewModel::loadAlerts) { Text("Retry") }
                    }
                    uiState.filtered.isEmpty() -> Column(
                        modifier            = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint     = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            if (uiState.filter == AlertFilter.Active) "No active alerts — all clear!"
                            else "No ${uiState.filter.label.lowercase()} alerts.",
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.filtered, key = { it.id }) { alert ->
                            AlertCard(
                                alert     = alert,
                                onResolve = { viewModel.resolve(alert.id) },
                                onDismiss = { viewModel.dismiss(alert.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: AlertDto,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (severityColor, severityBg) = when (alert.severity) {
        "Critical" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        "High"     -> Color(0xFFE65100) to Color(0xFFFFF3E0)
        "Medium"   -> Color(0xFFF9A825) to Color(0xFFFFFDE7)
        else       -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Severity bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = severityColor,
            ) {}

            Column(Modifier.padding(16.dp)) {
                // Header row
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = severityBg,
                        modifier = Modifier.padding(end = 10.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint     = severityColor,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(8.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            alert.type,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = severityColor,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeverityBadge(alert.severity, severityColor, severityBg)
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(alert.status)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(alert.message, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(alert.patientName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatVitalTimestamp(alert.triggeredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                alert.resolvedAt?.let { at ->
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Resolved ${formatVitalTimestamp(at)}" +
                                (alert.resolvedByName?.let { " by $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                        )
                    }
                }

                if (alert.status == "Active") {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick  = onResolve,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Resolve")
                        }
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: String, color: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            severity,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color    = color,
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        "Active"    -> MaterialTheme.colorScheme.error
        "Resolved"  -> Color(0xFF2E7D32)
        "Dismissed" -> MaterialTheme.colorScheme.onSurfaceVariant
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        "• $status",
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
