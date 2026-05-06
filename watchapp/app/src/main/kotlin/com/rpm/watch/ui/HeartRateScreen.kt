package com.rpm.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.rpm.watch.WatchUiState
import com.rpm.watch.WatchViewModel
import com.rpm.watch.health.HrStatus
import com.rpm.watch.service.ServiceStatus

@Composable
fun HeartRateScreen(viewModel: WatchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HeartRateContent(state = state, onToggle = {
        if (state.isMonitoring) viewModel.stopMonitoring()
        else viewModel.startMonitoring()
    })
}

@Composable
fun HeartRateContent(
    state: WatchUiState,
    onToggle: () -> Unit
) {
    Scaffold(
        timeText  = { TimeText() },
        modifier  = Modifier.background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Heart rate value ──────────────────────────────────────────────
            if (state.serviceStatus == ServiceStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier         = Modifier.size(40.dp),
                    indicatorColor   = MaterialTheme.colors.primary,
                    strokeWidth      = 3.dp
                )
            } else {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text       = "❤",
                        fontSize   = 22.sp,
                        color      = heartColor(state.hrStatus),
                        modifier   = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text       = if (state.heartRate > 0) "${state.heartRate}" else "--",
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color      = heartColor(state.hrStatus),
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text     = " bpm",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Status line ───────────────────────────────────────────────────
            Text(
                text     = buildStatusText(state),
                fontSize = 11.sp,
                color    = statusColor(state),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // ── Toggle button ─────────────────────────────────────────────────
            Button(
                onClick = onToggle,
                colors  = ButtonDefaults.buttonColors(
                    backgroundColor = if (state.isMonitoring)
                        MaterialTheme.colors.error
                    else
                        MaterialTheme.colors.primary
                ),
                modifier = Modifier.size(width = 90.dp, height = 36.dp)
            ) {
                Text(
                    text     = if (state.isMonitoring) "Stop" else "Start",
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun buildStatusText(state: WatchUiState): String {
    if (state.serviceStatus == ServiceStatus.ERROR && state.errorMessage.isNotBlank()) {
        return state.errorMessage
    }

    val sensorStr = when (state.hrStatus) {
        HrStatus.GOOD         -> "Good"
        HrStatus.MOVING       -> "Moving"
        HrStatus.DEVICE_MOVING -> "Still"
        HrStatus.LOW_PASS     -> "Low"
        HrStatus.INITIAL      -> "Place watch firmly"
    }
    return if (state.isMonitoring) sensorStr else "Tap Start to monitor"
}

private fun heartColor(status: HrStatus): Color = when (status) {
    HrStatus.GOOD         -> Color(0xFFE53935)   // red
    HrStatus.MOVING       -> Color(0xFFFF8F00)   // amber
    else                  -> Color(0xFF9E9E9E)   // grey
}

private fun statusColor(state: WatchUiState): Color =
    if (state.serviceStatus == ServiceStatus.ERROR) Color(0xFFFF5252) else Color(0xFFBDBDBD)
