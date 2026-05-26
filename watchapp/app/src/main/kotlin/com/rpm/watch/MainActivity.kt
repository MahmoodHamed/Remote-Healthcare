package com.rpm.watch

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpm.watch.service.HeartRateMonitorService
import com.rpm.watch.ui.HeartRateScreen
import com.rpm.watch.ui.SettingsScreen
import com.rpm.watch.ui.theme.WatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WatchViewModel by viewModels()

    // ── Runtime permissions ───────────────────────────────────────────────────
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.BODY_SENSORS] == true) {
                bindToService()
            }
        }

    // ── Service binding (for state injection into ViewModel) ──────────────────
    private var monitorService: HeartRateMonitorService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? HeartRateMonitorService.LocalBinder ?: return
            monitorService = localBinder.getService()
            monitorService?.let { viewModel.attachService(it) }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            monitorService = null
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showSettings by mutableStateOf(false)
            val patientId by viewModel.uiState.collectAsStateWithLifecycle()
            val mqttHost by viewModel.mqttHost.collectAsStateWithLifecycle()
            val mqttPort by viewModel.mqttPort.collectAsStateWithLifecycle()

            WatchTheme {
                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        patientId = patientId.patientId,
                        mqttHost = mqttHost,
                        mqttPort = mqttPort,
                        onBack = { showSettings = false }
                    )
                } else {
                    HeartRateScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }

        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unbindService(serviceConnection) } catch (_: Exception) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkAndRequestPermissions() {
        val perms = buildList {
            add(Manifest.permission.BODY_SENSORS)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add("android.permission.BODY_SENSORS_BACKGROUND")
            }
        }
        requestPermissions.launch(perms.toTypedArray())
    }

    private fun bindToService() {
        val intent = Intent(this, HeartRateMonitorService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
