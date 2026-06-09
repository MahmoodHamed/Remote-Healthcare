package com.rpm.watch.sensor

import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "SamsungHealthLauncher"

/**
 * Opens Samsung Health Monitor on the watch (ECG / BP UI).
 *
 * Samsung does not expose a public deep link or callback to read ECG results
 * back into third-party apps — measurements stay in Samsung Health Monitor.
 */
object SamsungHealthLauncher {

    private const val HEALTH_MONITOR_PACKAGE = "com.samsung.android.shealthmonitor"

    fun isHealthMonitorInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(HEALTH_MONITOR_PACKAGE) != null

    /** @return true if Samsung Health Monitor was launched */
    fun openHealthMonitor(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(HEALTH_MONITOR_PACKAGE)
        if (launch == null) {
            Log.w(TAG, "Samsung Health Monitor not installed ($HEALTH_MONITOR_PACKAGE)")
            return false
        }
        return try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            Log.i(TAG, "Launched Samsung Health Monitor")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Samsung Health Monitor: ${e.message}")
            false
        }
    }
}
