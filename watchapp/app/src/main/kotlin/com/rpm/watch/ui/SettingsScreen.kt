package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.rpm.watch.WatchViewModel

@Composable
fun SettingsScreen(
    viewModel: WatchViewModel,
    patientId: String,
    mqttHost: String,
    mqttPort: Int,
    onBack: () -> Unit
) {
    var patientInput by remember(patientId) { mutableStateOf(patientId) }
    var hostInput by remember(mqttHost) { mutableStateOf(mqttHost) }
    var portInput by remember(mqttPort) { mutableStateOf(mqttPort.toString()) }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Server setup",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(Modifier.height(6.dp))

            SettingsField(label = "Patient ID (6 chars)", value = patientInput, onValueChange = { patientInput = it })
            SettingsField(label = "MQTT host", value = hostInput, onValueChange = { hostInput = it })
            SettingsField(label = "MQTT port", value = portInput, onValueChange = { portInput = it.filter { ch -> ch.isDigit() } })

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val port = portInput.toIntOrNull() ?: 1883
                    viewModel.saveConfig(patientInput.trim(), hostInput.trim(), port)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Text("Save", fontSize = 12.sp)
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Back", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFBDBDBD),
            modifier = Modifier.fillMaxWidth()
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colors.onBackground,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(vertical = 6.dp, horizontal = 8.dp)
        )
    }
}
