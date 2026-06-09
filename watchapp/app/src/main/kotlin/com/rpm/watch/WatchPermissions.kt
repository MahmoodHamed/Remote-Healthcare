package com.rpm.watch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.rpm.watch.sensor.SensorType

/**
 * Runtime permissions per [Samsung permission guide](https://developer.samsung.com/health/sensor/guide/permission-request.html).
 */
object WatchPermissions {

    const val READ_SKIN_TEMPERATURE = "android.permission.health.READ_SKIN_TEMPERATURE"

    const val READ_ADDITIONAL_HEALTH_DATA =
        "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA"

    /** Samsung: Android 16+ (API 36) uses Health skin-temp permission; API 35- uses BODY_SENSORS. */
    private const val API_SAMSUNG_SKIN_TEMP_HEALTH_PERM = 36

    fun requiredForMonitoring(sensor: SensorType): List<String> = buildList {
        add(Manifest.permission.BODY_SENSORS)
        add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= API_SAMSUNG_SKIN_TEMP_HEALTH_PERM) {
            add(READ_ADDITIONAL_HEALTH_DATA)
            if (sensor == SensorType.SKIN_TEMPERATURE) {
                add(READ_SKIN_TEMPERATURE)
            }
        }
    }.distinct()

    fun optional(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.BODY_SENSORS_BACKGROUND)
        }
    }

    fun forAppLaunch(): List<String> =
        (
            requiredForMonitoring(SensorType.HEART_RATE) +
                requiredForMonitoring(SensorType.SKIN_TEMPERATURE) +
                requiredForMonitoring(SensorType.SPO2) +
                optional()
            ).distinct()

    fun missing(context: Context, sensor: SensorType): List<String> =
        notGranted(context, requiredForMonitoring(sensor))

    fun missingForAllVitals(context: Context): List<String> =
        notGranted(
            context,
            forAppLaunch().filter { it != Manifest.permission.POST_NOTIFICATIONS },
        )

    fun notGranted(context: Context, permissions: List<String>): List<String> =
        permissions.filter { permission ->
            isDeclared(context, permission) &&
                ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }

    fun hasAll(context: Context, sensor: SensorType): Boolean =
        missing(context, sensor).isEmpty()

    fun hasAllForVitals(context: Context): Boolean =
        missingForAllVitals(context).isEmpty()

    private fun isDeclared(context: Context, permission: String): Boolean = try {
        context.packageManager.getPermissionInfo(permission, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun label(permission: String): String = when (permission) {
        Manifest.permission.BODY_SENSORS -> "Body sensors"
        Manifest.permission.ACTIVITY_RECOGNITION -> "Activity recognition"
        READ_SKIN_TEMPERATURE -> "Skin temperature"
        READ_ADDITIONAL_HEALTH_DATA -> "Samsung health data"
        Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
        Manifest.permission.BODY_SENSORS_BACKGROUND -> "Background sensors"
        else -> permission.substringAfterLast('.')
    }

    fun deniedMessage(sensor: SensorType, denied: Collection<String> = emptyList()): String {
        val names = denied.map { label(it) }.distinct()
        if (names.isEmpty()) {
            return when (sensor) {
                SensorType.SKIN_TEMPERATURE -> "Tap Start → Allow Body sensors & Activity"
                else -> "Tap Start → Allow permissions"
            }
        }
        return "Tap Start → Allow ${names.joinToString(", ")}"
    }
}
