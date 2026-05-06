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
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

private const val TAG = "HRMonitorService"
private const val CHANNEL_ID = "rpm_watch_hr"
private const val NOTIFICATION_ID = 1
private const val MQTT_PUBLISH_INTERVAL_MS = 5_000L  // publish at most once every 5 s

enum class ServiceStatus { IDLE, CONNECTING, MEASURING, ERROR }

@AndroidEntryPoint
class HeartRateMonitorService : Service() {

    @Inject lateinit var hrTrackerManager: HeartRateTrackerManager
    @Inject lateinit var mqttManager: MqttManager
    @Inject lateinit var dataStore: WatchDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private val binder = LocalBinder()

    // ── Public state flows (read by ViewModel / UI) ───────────────────────────
    private val _heartRate   = MutableStateFlow(0)
    private val _hrStatus    = MutableStateFlow(HrStatus.INITIAL)
    private val _svcStatus   = MutableStateFlow(ServiceStatus.IDLE)
    private val _lastError   = MutableStateFlow("")

    val heartRate:  StateFlow<Int>           = _heartRate
    val hrStatus:   StateFlow<HrStatus>      = _hrStatus
    val svcStatus:  StateFlow<ServiceStatus> = _svcStatus
    val lastError:  StateFlow<String>        = _lastError

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        mqttManager.disconnect()
        _svcStatus.value = ServiceStatus.IDLE
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): HeartRateMonitorService = this@HeartRateMonitorService
    }

    // ── Monitoring logic ──────────────────────────────────────────────────────

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        _svcStatus.value = ServiceStatus.CONNECTING
        _lastError.value = ""

        monitorJob = serviceScope.launch {
            // 1. Fetch stored config
            val patientIdRaw = dataStore.getPatientId()
            val deviceId  = dataStore.getOrCreateDeviceId()
            val mqttHost  = dataStore.getMqttHost()
            val mqttPort  = dataStore.getMqttPort()
            val localSensorOnly = BuildConfig.LOCAL_SENSOR_ONLY

            if (patientIdRaw.isNullOrBlank()) {
                Log.e(TAG, "No patient ID configured — aborting service")
                _svcStatus.value = ServiceStatus.ERROR
                _lastError.value = "Patient ID is not set"
                updateNotification("Error: patient ID not set")
                return@launch
            }

            val patientId = normalizeGuid(patientIdRaw)
            if (patientId != patientIdRaw) {
                Log.w(TAG, "Patient ID is not GUID, using normalized GUID fallback: $patientId")
            }

            // After the check above, patientId is smart-cast to String (non-null)

            // 2. Connect MQTT (skip completely in local sensor-only mode)
            if (!localSensorOnly) {
                try {
                    mqttManager.connect(
                        host     = mqttHost,
                        port     = mqttPort,
                        clientId = "rpm-watch-$deviceId"
                    )
                    Log.i(TAG, "MQTT connected")
                } catch (e: Exception) {
                    Log.w(TAG, "MQTT unavailable, continuing local sensor mode: ${e.message}")
                    updateNotification("Starting sensor mode…")
                }
            } else {
                Log.i(TAG, "LOCAL_SENSOR_ONLY enabled: skipping MQTT connect")
            }

            // 3. Start Samsung Health / heart rate flow
            val topic = "vitals/$patientId/data"
            var lastPublishMs = 0L

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
                        _hrStatus.value  = reading.status
                        updateNotification("HR: ${reading.bpm} bpm")

                        // Rate-limit MQTT publishing
                        val now = System.currentTimeMillis()
                        if (!localSensorOnly && now - lastPublishMs >= MQTT_PUBLISH_INTERVAL_MS) {
                            lastPublishMs = now
                            publishReading(patientId, deviceId, reading.bpm, topic)
                        }
                    }

                    is TrackerState.Error -> {
                        _svcStatus.value = ServiceStatus.ERROR
                        _lastError.value = state.message
                        updateNotification("Sensor error: ${state.message}")
                        // Retry after delay
                        delay(5_000L)
                    }

                    is TrackerState.Disconnected -> {
                        _svcStatus.value = ServiceStatus.IDLE
                        _lastError.value = ""
                        updateNotification("Sensor disconnected")
                    }
                }
            }
        }
    }

    private fun publishReading(patientId: String, deviceId: String, bpm: Int, topic: String) {
        val payload = VitalsPayload(
            patientId    = patientId,
            deviceId     = deviceId,
            heartRateBpm = bpm.toFloat(),
            isWearing    = true
        )
        val json = Json.encodeToString(payload)
        mqttManager.publish(topic, json)
        Log.d(TAG, "Published HR $bpm bpm to $topic")
    }

    private fun normalizeGuid(value: String): String = try {
        UUID.fromString(value).toString()
    } catch (_: Exception) {
        UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Heart Rate Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Continuous heart rate monitoring and MQTT publishing"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RPM – Heart Rate")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // replace with custom icon
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        const val ACTION_START = "com.rpm.watch.START_HR"
        const val ACTION_STOP  = "com.rpm.watch.STOP_HR"

        fun startIntent(context: Context) =
            Intent(context, HeartRateMonitorService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, HeartRateMonitorService::class.java).apply { action = ACTION_STOP }
    }
}
