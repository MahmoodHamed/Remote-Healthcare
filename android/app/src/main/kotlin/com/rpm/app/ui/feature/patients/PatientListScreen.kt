package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.PatientSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onPatientClick: (patientId: String) -> Unit,
    onLogout: () -> Unit,
    viewModel: PatientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.loadPatients() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
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
                uiState.patients.isEmpty() -> Text(
                    "No patients assigned yet.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(uiState.patients) { patient ->
                        PatientCard(patient, onClick = { onPatientClick(patient.patientId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(patient: PatientSummaryDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
                patient.latestVitals?.let { v ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            v.heartRateBpm?.let { append("HR: ${it.toInt()} bpm  ") }
                            v.spO2Percent?.let { append("SpO2: ${it.toInt()}%") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (patient.unresolvedAlertCount > 0) {
                Badge { Text(patient.unresolvedAlertCount.toString()) }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
