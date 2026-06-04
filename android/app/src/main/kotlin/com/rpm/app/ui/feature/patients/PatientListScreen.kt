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
    title: String,
    emptyMessage: String,
    userRole: String?,
    onPatientClick: (patientId: String) -> Unit,
    onLogout: () -> Unit,
    autoOpenSinglePatient: Boolean = false,
    viewModel: PatientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var didAutoOpen by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.patients, autoOpenSinglePatient, uiState.isLoading) {
        if (!didAutoOpen && autoOpenSinglePatient && !uiState.isLoading && uiState.patients.size == 1) {
            didAutoOpen = true
            onPatientClick(uiState.patients.first().userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                    emptyMessage,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(uiState.patients) { patient ->
                        PatientCard(
                            patient = patient,
                            userRole = userRole,
                            onClick = { onPatientClick(patient.userId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(patient: PatientSummaryDto, userRole: String?, onClick: () -> Unit) {
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
                Spacer(Modifier.height(4.dp))
                Text(
                    when (userRole) {
                        "Doctor" -> "Patient vitals"
                        "Relative" -> "Family member vitals"
                        else -> "Latest vitals"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                patient.latestVitals?.let { v ->
                    Spacer(Modifier.height(2.dp))
                    VitalsSummaryText(v)
                } ?: Text(
                    "No vitals yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                patient.bloodType?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Blood type: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
