package com.rpm.watch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.rpm.watch.BuildConfig
import com.rpm.watch.MainActivity
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.health.HeartRateTrackerManager
import com.rpm.watch.health.HrStatus
import com.rpm.watch.health.TrackerState
import com.rpm.watch.health.WatchSensorsManager
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.mqtt.VitalsPayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

private const val TAG = "HRMonitorService"
private const val CHANNEL_ID = "rpm_watch_hr"
private const val NOTIFICATION_ID = 1
/** Publish latest vitals to MQTT on this cadence (decoupled from Samsung batching). */
private const val MQTT_PUBLISH_TICK_MS = 1_000L

enum class ServiceStatus { IDLE, CONNECTING, MEASURING, ERROR }

@AndroidEntryPoint
class HeartRateMonitorService : Service() {

    @Inject lateinit var hrTrackerManager: HeartRateTrackerManager
    @Inject lateinit var sensorsManager: WatchSensorsManager
    @Inject lateinit var mqttManager: MqttManager
    @Inject lateinit var dataStore: WatchDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private val binder = LocalBinder()

    @Volatile private var lastHeartRate: Float? = null
    @Volatile private var maxHeartRate: Float? = null
    @Volatile private var isWearingNow: Boolean = true

    private val _heartRate = MutableStateFlow(0)
    private val _hrStatus = MutableStateFlow(HrStatus.INITIAL)
    private val _svcStatus = MutableStateFlow(ServiceStatus.IDLE)
    private val _lastError = MutableStateFlow("")

    val heartRate: StateFlow<Int> = _heartRate
    val hrStatus: StateFlow<HrStatus> = _hrStatus
    val svcStatus: StateFlow<ServiceStatus> = _svcStatus
    val lastError: StateFlow<String> = _lastError

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorsManager.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        mqttManager.disconnect()
        sensorsManager.stop()
        _svcStatus.value = ServiceStatus.IDLE
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): HeartRateMonitorService = this@HeartRateMonitorService
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        _svcStatus.value = ServiceStatus.CONNECTING
        _lastError.value = ""

        monitorJob = serviceScope.launch {
            val patientIdRaw = dataStore.getPatientId()
            val deviceId = dataStore.getOrCreateDeviceId()
            val mqttHost = dataStore.getMqttHost()
            val mqttPort = dataStore.getMqttPort()
            val localSensorOnly = BuildConfig.LOCAL_SENSOR_ONLY

            if (patientIdRaw.isNullOrBlank()) {
                Log.e(TAG, "No patient ID configured")
                _svcStatus.value = ServiceStatus.ERROR
                _lastError.value = "Patient ID is not set"
                updateNotification("Error: patient ID not set")
                return@launch
            }

            val patientId = normalizeGuid(patientIdRaw)
            val topic = "vitals/$patientId/data"

            if (!localSensorOnly) {
                try {
                    mqttManager.connect(mqttHost, mqttPort, "rpm-watch-$deviceId")
                } catch (e: Exception) {
                    Log.w(TAG, "MQTT unavailable, continuing local sensor mode: ${e.message}")
                }
            }

            val publishTicker = launch {
                while (isActive) {
                    delay(MQTT_PUBLISH_TICK_MS)
                    val hr = lastHeartRate
                    if (!localSensorOnly && hr != null && hr > 0f) {
                        publishReading(patientId, deviceId, topic)
                    }
                }
            }

            try {
            hrTrackerManager.heartRateFlow().collect { state ->
                when (state) {
                    is TrackerState.Connecting -> {
                        _svcStatus.value = ServiceStatus.CONNECTING
                        _lastError.value = ""
                        updateNotification("Connecting to sensor…")
                    }
                    is TrackerState.Measuring -> {
                        _svcStatus.value = ServiceStatus.MEASURING
                        _lastError.value = ""
                        val reading = state.reading
                        _heartRate.value = reading.bpm
                        _hrStatus.value = reading.status
                        lastHeartRate = reading.bpm.toFloat()
                        maxHeartRate = maxOf(maxHeartRate ?: 0f, reading.bpm.toFloat())
                        isWearingNow = reading.status != HrStatus.DEVICE_MOVING
                        updateNotification("HR: ${reading.bpm} bpm")

                        if (!localSensorOnly && reading.bpm > 0) {
                            publishReading(patientId, deviceId, topic)
                        }
                    }
                    is TrackerState.Error -> {
                        _svcStatus.value = ServiceStatus.ERROR
                        _lastError.value = state.message
                        updateNotification("Sensor error: ${state.message}")
                        delay(5_000L)
                    }
                    is TrackerState.Disconnected -> {
                        _svcStatus.value = ServiceStatus.IDLE
                        _lastError.value = ""
                        updateNotification("Sensor disconnected")
                    }
                }
            }
            } finally {
                publishTicker.cancel()
            }
        }
    }

    private fun publishReading(patientId: String, deviceId: String, topic: String) {
        val sensors = sensorsManager.snapshot()
        val payload = VitalsPayload(
            patientId = patientId,
            deviceId = deviceId,
            heartRateBpm = lastHeartRate,
            heartRateVariabilityMs = sensors.heartRateVariabilityMs,
            maxHeartRateBpm = maxHeartRate,
            skinTemperatureC = sensors.skinTemperatureC,
            temperatureC = sensors.ambientTemperatureC,
            stepsCount = sensors.stepsCount,
            caloriesBurned = sensors.caloriesBurned,
            distanceMeters = sensors.distanceMeters,
            batteryLevel = sensors.batteryLevel,
            fallDetected = sensors.fallDetected,
            isWearing = isWearingNow,
        )
        val json = Json.encodeToString(payload)
        mqttManager.publish(topic, json)
        Log.d(TAG, "Published vitals payload to $topic")
    }

    private fun normalizeGuid(value: String): String = try {
        UUID.fromString(value).toString()
    } catch (_: Exception) {
        val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        fun byteHex(b: Byte) = "%02x".format(b.toInt() and 0xff)
        buildString(36) {
            for (i in bytes.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                append(byteHex(bytes[i]))
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Heart Rate Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Continuous heart rate monitoring and MQTT publishing"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RPM – Vitals stream")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.rpm.watch.START_HR"
        const val ACTION_STOP = "com.rpm.watch.STOP_HR"

        fun startIntent(context: Context) =
            Intent(context, HeartRateMonitorService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, HeartRateMonitorService::class.java).apply { action = ACTION_STOP }
    }
}
