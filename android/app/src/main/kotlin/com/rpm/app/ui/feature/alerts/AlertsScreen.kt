package com.rpm.app.ui.feature.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rpm.app.data.remote.dto.AlertDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.alerts.isEmpty() -> Text(
                    "No alerts found.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(uiState.alerts) { alert ->
                        AlertCard(
                            alert = alert,
                            onResolve = { viewModel.resolve(alert.id) },
                            onDismiss = { viewModel.dismiss(alert.id) }
                        )
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
    onDismiss: () -> Unit
) {
    val severityColor = when (alert.severity) {
        "Critical"  -> MaterialTheme.colorScheme.error
        "High"      -> Color(0xFFF57C00)
        "Medium"    -> Color(0xFFFDD835)
        else        -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = severityColor)
                Spacer(Modifier.width(8.dp))
                Text(alert.type, style = MaterialTheme.typography.titleMedium, color = severityColor)
                Spacer(Modifier.weight(1f))
                Badge(containerColor = severityColor) { Text(alert.severity) }
            }
            Spacer(Modifier.height(4.dp))
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Patient: ${alert.patientName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                alert.triggeredAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alert.status == "Active") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onResolve, modifier = Modifier.weight(1f)) { Text("Resolve") }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Status: ${alert.status}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
