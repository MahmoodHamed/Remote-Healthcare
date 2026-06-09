package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.rpm.watch.BuildConfig
import com.rpm.watch.WatchViewModel

@Composable
fun SettingsScreen(
    viewModel: WatchViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()
    var patientId by remember(state.patientId) {
        mutableStateOf(state.patientId.ifBlank { BuildConfig.DEFAULT_PATIENT_ID })
    }
    var mqttHost by remember(state.mqttHost) { mutableStateOf(state.mqttHost) }
    var mqttPort by remember(state.mqttPort) { mutableStateOf(state.mqttPort) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Enter the same 6-character Patient ID you saved in the mobile app (e.g. ABC123)",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SettingField(
                    label = "Patient ID",
                    value = patientId,
                    onValueChange = { v -> patientId = v.filter { it.isLetterOrDigit() }.take(6).uppercase(); saved = false },
                    keyboardType = KeyboardType.Ascii,
                )
            }
            item {
                SettingField(label = "MQTT Host", value = mqttHost, onValueChange = { mqttHost = it; saved = false })
            }
            item {
                SettingField(
                    label = "MQTT Port",
                    value = mqttPort,
                    onValueChange = { mqttPort = it.filter { c -> c.isDigit() }; saved = false },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                Button(
                    onClick = {
                        viewModel.saveSettings(patientId, mqttHost, mqttPort.toIntOrNull() ?: 1883)
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = patientId.isNotBlank() && mqttHost.isNotBlank(),
                ) {
                    Text(if (saved) "Saved" else "Save")
                }
            }
            item {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
            item {
                Text(
                    "Shared vitals (Samsung SDK)",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            SupportedWatchVitals.sdkContinuous.forEach { line ->
                item {
                    Text("• $line", fontSize = 8.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Text(
                    "On-demand",
                    fontSize = 8.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            SupportedWatchVitals.sdkOnDemand.forEach { line ->
                item {
                    Text("• $line", fontSize = 8.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Text(
                    "Platform",
                    fontSize = 8.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            SupportedWatchVitals.platformSensors.forEach { line ->
                item {
                    Text("• $line", fontSize = 8.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            if (state.wasMonitoring) {
                item {
                    Text(
                        "Restart monitoring after saving.",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Text(label, fontSize = 10.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.body1.copy(fontSize = 11.sp),
    )
}
