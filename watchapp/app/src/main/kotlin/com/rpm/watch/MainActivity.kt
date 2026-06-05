package com.rpm.watch



import android.content.ComponentName

import android.content.Context

import android.content.Intent

import android.content.ServiceConnection

import android.net.Uri

import android.os.Bundle

import android.os.IBinder

import android.provider.Settings

import android.util.Log

import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.viewModels

import com.rpm.watch.sensor.SensorType

import com.rpm.watch.service.VitalsMonitorService

import com.rpm.watch.ui.VitalsMonitorScreen

import com.rpm.watch.ui.theme.WatchTheme

import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint

class MainActivity : ComponentActivity() {



    private val viewModel: WatchViewModel by viewModels()



    private var permissionGrantedCallback: (() -> Unit)? = null



    private val requestPermissions =

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            Log.i(TAG, "Permission granted=$granted")

            val stillNeeded = WatchPermissions.missingForAllVitals(this)

            if (stillNeeded.isEmpty()) {

                viewModel.onPermissionsGranted()

                permissionGrantedCallback?.invoke()

                permissionGrantedCallback = null

            } else if (!granted) {

                viewModel.onPermissionsDenied(stillNeeded.toSet())

                permissionGrantedCallback = null

            } else {

                requestNextPermission()

            }

        }



    private var monitorService: VitalsMonitorService? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {

            val localBinder = binder as? VitalsMonitorService.LocalBinder ?: return

            monitorService = localBinder.getService()

            monitorService?.let { viewModel.attachService(it) }

        }

        override fun onServiceDisconnected(name: ComponentName?) {

            monitorService = null

        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        viewModel.onRequestBindService = { bindMonitoringService() }

        viewModel.onRequestPermissions = { _, onReady ->
            ensureAllVitalsPermissions(onReady)
        }

        viewModel.onRequestEcg = {
            monitorService?.requestEcgMeasurement()
        }



        setContent {

            WatchTheme {

                VitalsMonitorScreen(viewModel = viewModel)

            }

        }



        requestLaunchPermissions()

        bindMonitoringService()

    }



    override fun onResume() {

        super.onResume()

        val missing = WatchPermissions.missingForAllVitals(this)

        if (missing.isEmpty()) {

            viewModel.onPermissionsGranted()

        } else if (!viewModel.uiState.value.isMonitoring) {

            viewModel.showPermissionReminder(missing)

        }

    }



    override fun onDestroy() {

        super.onDestroy()

        try { unbindService(serviceConnection) } catch (_: Exception) {}

    }



    private fun requestLaunchPermissions() {

        if (WatchPermissions.missingForAllVitals(this).isNotEmpty()) {

            requestNextPermission(onComplete = null)

        }

    }



    fun ensureAllVitalsPermissions(onReady: () -> Unit) {

        if (WatchPermissions.hasAllForVitals(this)) {

            viewModel.onPermissionsGranted()

            onReady()

            return

        }

        requestNextPermission(onComplete = onReady)

    }



    private fun requestNextPermission(onComplete: (() -> Unit)? = null) {

        permissionGrantedCallback = onComplete

        val stillNeeded = WatchPermissions.missingForAllVitals(this)

        if (stillNeeded.isEmpty()) {

            viewModel.onPermissionsGranted()

            onComplete?.invoke()

            permissionGrantedCallback = null

            return

        }

        val next = stillNeeded.first()

        Log.i(TAG, "Requesting: $next (${WatchPermissions.label(next)})")

        requestPermissions.launch(next)

    }



    fun openAppSettings() {

        startActivity(

            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {

                data = Uri.fromParts("package", packageName, null)

            },

        )

    }



    fun bindMonitoringService() {

        monitorService?.let {

            viewModel.attachService(it)

            return

        }

        try {

            bindService(

                Intent(this, VitalsMonitorService::class.java),

                serviceConnection,

                Context.BIND_AUTO_CREATE,

            )

        } catch (e: Exception) {

            Log.e(TAG, "Failed to bind vitals service", e)

        }

    }



    companion object {

        private const val TAG = "MainActivity"

    }

}


