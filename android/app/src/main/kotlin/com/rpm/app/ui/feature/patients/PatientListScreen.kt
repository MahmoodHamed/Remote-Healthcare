package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    modifier: Modifier = Modifier,
    viewModel: PatientListViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsState()
    var didAutoOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.patients, autoOpenSinglePatient, uiState.isLoading) {
        if (!didAutoOpen && autoOpenSinglePatient && !uiState.isLoading && uiState.patients.size == 1) {
            didAutoOpen = true
            onPatientClick(uiState.patients.first().userId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar   = {
            TopAppBar(
                title  = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        if (!uiState.isLoading && uiState.patients.isNotEmpty()) {
                            Text(
                                "${uiState.patients.size} patient${if (uiState.patients.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPatients() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Column(
                    modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = { viewModel.loadPatients() }) { Text("Retry") }
                }
                uiState.patients.isEmpty() -> Column(
                    modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.PersonOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        emptyMessage,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        style     = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.patients) { patient ->
                        PatientCard(
                            patient  = patient,
                            userRole = userRole,
                            onClick  = { onPatientClick(patient.userId) },
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
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier  = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with initials
            val initials = patient.fullName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials.ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    patient.fullName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(4.dp))

                // Vitals summary or chips
                if (patient.latestVitals != null) {
                    VitalsSummaryText(patient.latestVitals)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WatchOff, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("No vitals yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    patient.bloodType?.let {
                        ChipLabel(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    patient.dateOfBirth?.let { dob ->
                        runCatching {
                            val age = java.time.Period.between(
                                java.time.LocalDate.parse(dob),
                                java.time.LocalDate.now(),
                            ).years
                            ChipLabel("$age yrs", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ChipLabel(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
        )
    }
}
