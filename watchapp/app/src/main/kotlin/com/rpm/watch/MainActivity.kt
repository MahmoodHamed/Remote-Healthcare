package com.rpm.watch

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.rpm.watch.service.HeartRateMonitorService
import com.rpm.watch.ui.HeartRateScreen
import com.rpm.watch.ui.theme.WatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WatchViewModel by viewModels()

    // ── Runtime permissions ───────────────────────────────────────────────────
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val bodySensorsGranted = results[Manifest.permission.BODY_SENSORS] == true
            if (bodySensorsGranted) bindToService()
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
            WatchTheme {
                HeartRateScreen(viewModel = viewModel)
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
        val needed = buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.BODY_SENSORS_BACKGROUND)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.filter { permission ->
            checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            bindToService()
        } else {
            requestPermissions.launch(needed.toTypedArray())
        }
    }

    private fun bindToService() {
        val intent = Intent(this, HeartRateMonitorService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
