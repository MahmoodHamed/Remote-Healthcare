package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    LaunchedEffect(state.saveSuccess, state.savedLocally) {
        if (state.saveSuccess) {
            val msg = if (state.savedLocally) {
                "Saved on this phone. Enter the same code on your watch."
            } else {
                "Pairing details saved"
            }
            snackbarHost.showSnackbar(msg)
            vm.clearSaveSuccess()
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
            item {
                PairingSetupCard(
                    shortCode = state.shortCodeInput,
                    pairingInfo = state.pairingInfo,
                    isSaving = state.isSaving,
                    savedLocally = state.savedLocally,
                    onShortCodeChange = vm::updateShortCode,
                    onSave = vm::savePairing,
                )
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
private fun PairingSetupCard(
    shortCode: String,
    pairingInfo: PairingInfoDto?,
    isSaving: Boolean,
    savedLocally: Boolean,
    onShortCodeChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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

            SetupStep(number = "1", title = "Choose your patient ID", body = "Type a 6-character code below (e.g. ABC123) and tap Save.")
            SetupStep(number = "2", title = "Enter it on the watch", body = "Open the watch app → Settings → Patient ID and type the same code.")
            SetupStep(number = "3", title = "Press start", body = "The watch streams vitals to your dashboard after the first reading.")

            Text(
                "Pairing details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            OutlinedTextField(
                value = shortCode,
                onValueChange = onShortCodeChange,
                label = { Text("Patient short ID") },
                placeholder = { Text("ABC123") },
                supportingText = {
                    Text("Must match the watch exactly. Save before starting the watch.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
            )

            if (!pairingInfo?.streamingPatientId.isNullOrBlank()) {
                OutlinedTextField(
                    value = pairingInfo!!.streamingPatientId,
                    onValueChange = {},
                    label = { Text("Streaming patient ID") },
                    supportingText = { Text("Internal ID used by the server after your short code is saved.") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                )
            }

            pairingInfo?.let { info ->
                OutlinedTextField(
                    value = info.mqttHost,
                    onValueChange = {},
                    label = { Text("MQTT host") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                )
                OutlinedTextField(
                    value = info.mqttPort.toString(),
                    onValueChange = {},
                    label = { Text("MQTT port") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                )
            }

            Button(
                onClick = onSave,
                enabled = !isSaving && shortCode.length == 6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving…" else "Save pairing details")
            }

            if (savedLocally) {
                Text(
                    "Saved on this device. Enter the same code on your watch. Device list updates after the server is updated and the watch sends its first reading.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun SetupStep(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.labelMedium.fontSize)
            }
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
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
                "Save your Patient ID here, enter the same code on the watch, then tap Start. The watch appears here after the first reading.",
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
