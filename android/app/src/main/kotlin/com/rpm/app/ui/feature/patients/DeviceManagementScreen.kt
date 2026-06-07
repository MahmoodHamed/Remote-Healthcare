package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.remote.dto.PairingInfoDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    onBack: () -> Unit,
    vm: DeviceManagementViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(state.renameSuccess) {
        if (state.renameSuccess) {
            snackbarHost.showSnackbar("Device renamed successfully")
            vm.clearRenameSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Watch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.pairingInfo?.let { info ->
                item { PairingInfoCard(info, state.usingLocalPairing) }
            }

            if (state.devices.isEmpty()) {
                item { NoDevicesCard() }
            } else {
                item {
                    Text(
                        "Linked Devices (${state.devices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.devices) { device ->
                    DeviceCard(device = device, onRename = { id, name -> vm.renameDevice(id, name) })
                }
            }
        }
    }
}

@Composable
private fun PairingInfoCard(info: PairingInfoDto, fromLocalFallback: Boolean) {
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Watch Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                "Enter this 6-character Patient ID in your Galaxy Watch app → Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Patient ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        info.patientId.uppercase(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { clipboard.setText(AnnotatedString(info.patientId.uppercase())) },
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy ID")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("MQTT Broker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            "${info.mqttHost}:${info.mqttPort}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(info.mqttHost)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy host", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (info.streamingPatientId.isNotBlank()) {
                Text(
                    "Streaming ID: ${info.streamingPatientId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            if (fromLocalFallback) {
                Text(
                    "Code generated from your account. Device list updates after the watch sends its first reading.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceDto, onRename: (String, String) -> Unit) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(deviceStatusColor(device.status)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.deviceName.takeIf { it != "unknown" } ?: device.deviceModel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (device.deviceModel != "unknown") {
                    Text(device.deviceModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(device.status)
                    device.batteryLevel?.let {
                        Text("Battery: ${it.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                device.lastSeenAt?.let {
                    Text("Last seen: ${formatDeviceTime(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showRenameDialog) {
        RenameDeviceDialog(
            currentName = device.deviceName.takeIf { it != "unknown" } ?: "",
            onConfirm = { newName -> onRename(device.id, newName); showRenameDialog = false },
            onDismiss = { showRenameDialog = false },
        )
    }
}

@Composable
private fun RenameDeviceDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Rename Device") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Device name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NoDevicesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.WatchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("No watch linked yet", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "Enter your Patient ID (e.g. ABC123) on the watch, then tap Start. The watch will appear here after the first reading.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "Online" -> "Online" to Color(0xFF4CAF50)
        "LowBattery" -> "Low Battery" to Color(0xFFFF9800)
        else -> "Offline" to Color(0xFF9E9E9E)
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun deviceStatusColor(status: String): Color = when (status) {
    "Online" -> Color(0xFF4CAF50)
    "LowBattery" -> Color(0xFFFF9800)
    else -> Color(0xFF9E9E9E)
}

private fun formatDeviceTime(iso: String): String = runCatching {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("MMM d, h:mm a").format(instant.atZone(ZoneId.systemDefault()))
}.getOrDefault(iso)
