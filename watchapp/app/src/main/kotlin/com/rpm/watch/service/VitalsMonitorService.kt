package com.rpm.watch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.rpm.watch.BuildConfig
import com.rpm.watch.MainActivity
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.mqtt.VitalsPayload
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.SensorType
import com.rpm.watch.sensor.TrackerState
import com.rpm.watch.sensor.VitalReading
import com.rpm.watch.sensor.VitalsSensorCoordinator
import com.rpm.watch.sensor.motion.MotionSensorHub
import com.rpm.watch.sensor.wear.WearDetectionHub
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
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

private const val TAG = "VitalsMonitorService"
private const val CHANNEL_ID = "rpm_watch_vitals"
private const val NOTIFICATION_ID = 1
private const val MQTT_PUBLISH_MIN_INTERVAL_MS = 500L
private const val MQTT_HEARTBEAT_INTERVAL_MS = 2_000L
private const val MQTT_CONNECT_TIMEOUT_MS = 5_000L

@AndroidEntryPoint
class VitalsMonitorService : Service() {

    @Inject lateinit var vitalsCoordinator: VitalsSensorCoordinator
    @Inject lateinit var mqttManager: MqttManager
    @Inject lateinit var dataStore: WatchDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var trackerCollectJob: Job? = null
    private val binder = LocalBinder()
    private lateinit var motionHub: MotionSensorHub
    private lateinit var wearHub: WearDetectionHub
    private var wearCollectJob: Job? = null
    private var mqttConnectJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile private var latestHeartRate: Int? = null
    @Volatile private var latestTemperatureC: Float? = null
    @Volatile private var latestSkinTemperatureC: Float? = null
    @Volatile private var latestAmbientTemperatureC: Float? = null
    @Volatile private var latestHrvMs: Float? = null
    @Volatile private var latestSpO2Percent: Float? = null
    @Volatile private var latestStressScore: Float? = null
    @Volatile private var latestBodyFatPercent: Float? = null
    @Volatile private var latestEcgAvgHeartRateBpm: Float? = null
    @Volatile private var isWearingNow: Boolean = true
    @Volatile private var offBodyOnWrist: Boolean? = null
    @Volatile private var confirmedOffWrist: Boolean = false

    @Volatile private var vitalsMqttTopic: String? = null
    @Volatile private var vitalsPatientId: String? = null
    @Volatile private var vitalsDeviceId: String? = null
    @Volatile private var vitalsMqttEnabled: Boolean = false
    private var lastMqttPublishMs: Long = 0L

    private val _heartRate = MutableStateFlow(0)
    private val _temperatureC = MutableStateFlow<Float?>(null)
    private val _spO2Percent = MutableStateFlow<Float?>(null)
    private val _heartRateStatus = MutableStateFlow(HeartRateStatus.INITIAL)
    private val _svcStatus = MutableStateFlow(VitalsServiceStatus.IDLE)
    private val _lastError = MutableStateFlow("")

    val heartRate: StateFlow<Int> = _heartRate
    val temperatureC: StateFlow<Float?> = _temperatureC
    val spO2Percent: StateFlow<Float?> = _spO2Percent
    val heartRateStatus: StateFlow<HeartRateStatus> = _heartRateStatus
    val svcStatus: StateFlow<VitalsServiceStatus> = _svcStatus
    val lastError: StateFlow<String> = _lastError
    val mqttState: StateFlow<MqttConnectionState>
        get() = mqttManager.connectionState

    override fun onCreate() {
        super.onCreate()
        motionHub = MotionSensorHub(this)
        wearHub = WearDetectionHub(this)
        createNotificationChannel()
        motionHub.start()
        wearHub.start()
        wearCollectJob = serviceScope.launch {
            wearHub.onWrist.collect { onWrist ->
                offBodyOnWrist = onWrist
                refreshWearingState()
            }
        }
        serviceScope.launch {
            wearHub.confirmedOffWrist.collect { offWrist ->
                confirmedOffWrist = offWrist
                refreshWearingState()
                if (offWrist && !hasSuccessfulHeartRate()) clearHeartRateDisplay()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring(readSensor(intent))
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        wearCollectJob?.cancel()
        mqttConnectJob?.cancel()
        releaseMeasurementWakeLock()
        mqttManager.disconnect()
        motionHub.stop()
        wearHub.stop()
        _svcStatus.value = VitalsServiceStatus.IDLE
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): VitalsMonitorService = this@VitalsMonitorService
    }

    private fun startMonitoring(@Suppress("UNUSED_PARAMETER") sensor: SensorType) {
        if (monitorJob?.isActive == true) {
            promoteToForeground("Monitoring vitals…")
            return
        }

        trackerCollectJob?.cancel()
        mqttConnectJob?.cancel()
        monitorJob?.cancel()

        acquireMeasurementWakeLock()
        promoteToForeground("Starting sensors…")
        _svcStatus.value = VitalsServiceStatus.CONNECTING
        _lastError.value = ""
        resetAllSensorState()

        monitorJob = serviceScope.launch {
            val patientIdRaw = dataStore.getPatientId()
            val deviceId = dataStore.getOrCreateDeviceId()
            val localSensorOnly = BuildConfig.LOCAL_SENSOR_ONLY

            if (patientIdRaw.isNullOrBlank()) {
                _svcStatus.value = VitalsServiceStatus.ERROR
                _lastError.value = "Patient ID is not set"
                updateNotification("Error: patient ID not set")
                releaseMeasurementWakeLock()
                return@launch
            }

            val patientId = normalizeGuid(patientIdRaw)
            val topic = "vitals/$patientId/data"
            vitalsMqttTopic = topic
            vitalsPatientId = patientId
            vitalsDeviceId = deviceId
            vitalsMqttEnabled = !localSensorOnly

            trackerCollectJob = launch {
                vitalsCoordinator.allVitalsFlow().collect { event ->
                    val sensorType = event.sensor
                    when (val state = event.state) {
                        is TrackerState.Connecting -> {
                            _svcStatus.value = VitalsServiceStatus.CONNECTING
                            _lastError.value = ""
                            updateNotification("Starting sensors…")
                        }
                        is TrackerState.Measuring -> {
                            _svcStatus.value = VitalsServiceStatus.MEASURING
                            _lastError.value = ""
                            applyReading(sensorType, state.reading)
                            updateNotification(statusMessage(sensorType, state.reading))
                        }
                        is TrackerState.Error -> {
                            _svcStatus.value = VitalsServiceStatus.ERROR
                            _lastError.value = state.message
                            updateNotification("Error: ${state.message}")
                        }
                        is TrackerState.Disconnected -> {
                            _svcStatus.value = VitalsServiceStatus.IDLE
                            _lastError.value = ""
                        }
                    }
                }
            }

            mqttConnectJob = launch {
                if (localSensorOnly) return@launch
                val mqttHost = dataStore.getMqttHost()
                val mqttPort = dataStore.getMqttPort()
                try {
                    withTimeout(MQTT_CONNECT_TIMEOUT_MS) {
                        mqttManager.connect(
                            host = mqttHost,
                            port = mqttPort,
                            clientId = "rpm-watch-$deviceId",
                        )
                    }
                    updateNotification("Sensors active — connected")
                    publishVitals(patientId, deviceId, topic)
                    lastMqttPublishMs = System.currentTimeMillis()
                } catch (e: Exception) {
                    _lastError.value = "Server unreachable — sensors still active"
                    updateNotification("Sensors active (no server)")
                    Log.w(TAG, "MQTT connect failed: ${e.message}")
                }
            }

            launch {
                while (isActive) {
                    delay(MQTT_HEARTBEAT_INTERVAL_MS)
                    maybePublishVitals(force = true)
                }
            }
        }
    }

    private fun resetAllSensorState() {
        latestHeartRate = null
        latestTemperatureC = null
        latestSkinTemperatureC = null
        latestAmbientTemperatureC = null
        latestHrvMs = null
        latestSpO2Percent = null
        _heartRate.value = 0
        _temperatureC.value = null
        _spO2Percent.value = null
        _heartRateStatus.value = HeartRateStatus.INITIAL
    }

    private fun acquireMeasurementWakeLock() {
        releaseMeasurementWakeLock()
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rpm:vitals").apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L)
        }
    }

    private fun releaseMeasurementWakeLock() {
        runCatching {
            wakeLock?.let { if (it.isHeld) it.release() }
        }
        wakeLock = null
    }

    private fun maybePublishVitals(force: Boolean = false) {
        val topic = vitalsMqttTopic ?: return
        val patientId = vitalsPatientId ?: return
        val deviceId = vitalsDeviceId ?: return
        if (!vitalsMqttEnabled) return
        if (mqttManager.connectionState.value != MqttConnectionState.CONNECTED) return

        val now = System.currentTimeMillis()
        if (!force && now - lastMqttPublishMs < MQTT_PUBLISH_MIN_INTERVAL_MS) return

        lastMqttPublishMs = now
        publishVitals(patientId, deviceId, topic)
    }

    private fun publishVitals(patientId: String, deviceId: String, topic: String) {
        val motion = motionHub.snapshot()
        val wearing = effectiveIsWearing()
        val hrForMqtt = latestHeartRate?.toFloat()?.takeIf {
            wearing && _heartRateStatus.value.isSuccessful
        }
        val payload = VitalsPayload(
            patientId = patientId,
            deviceId = deviceId,
            heartRateBpm = hrForMqtt,
            spO2Percent = latestSpO2Percent,
            temperatureC = latestTemperatureC,
            skinTemperatureC = latestSkinTemperatureC,
            ambientTemperatureC = latestAmbientTemperatureC,
            hrvMs = latestHrvMs?.takeIf { wearing },
            stressScore = latestStressScore,
            bodyFatPercent = latestBodyFatPercent,
            ecgAvgHeartRateBpm = latestEcgAvgHeartRateBpm,
            stepsCount = motion.stepsCount,
            caloriesBurned = motion.caloriesBurned,
            fallDetected = motion.fallDetected,
            isWearing = wearing,
        )
        mqttManager.publish(topic, Json.encodeToString(payload))
        Log.d(
            TAG,
            "Published vitals hr=$latestHeartRate skin=$latestSkinTemperatureC hrv=$latestHrvMs spo2=$latestSpO2Percent",
        )
    }

    private fun applyReading(sensor: SensorType, reading: VitalReading) {
        when (sensor) {
            SensorType.HEART_RATE -> {
                _heartRateStatus.value = reading.status
                refreshWearingState()
                if (reading.status == HeartRateStatus.DETACHED) {
                    clearHeartRateDisplay()
                    maybePublishVitals()
                    return
                }
                reading.hrvMs?.let { latestHrvMs = it }
                val bpm = reading.heartRateBpm
                if (bpm != null && reading.status.isSuccessful && effectiveIsWearing()) {
                    latestHeartRate = bpm
                    _heartRate.value = bpm
                    maybePublishVitals()
                }
            }
            SensorType.EDA -> {
                reading.stressScore?.let {
                    latestStressScore = it
                    maybePublishVitals()
                }
            }
            SensorType.BIA -> {
                reading.bodyFatPercent?.let {
                    latestBodyFatPercent = it
                    maybePublishVitals()
                }
            }
            SensorType.ECG -> {
                if (reading.ecgComplete) {
                    latestEcgAvgHeartRateBpm = reading.ecgAvgHeartRateBpm
                        ?: latestHeartRate?.toFloat()?.takeIf { effectiveIsWearing() }
                    maybePublishVitals()
                }
            }
            SensorType.SPO2 -> {
                val pct = reading.spO2Percent ?: return
                latestSpO2Percent = pct
                _spO2Percent.value = pct
                maybePublishVitals()
            }
            SensorType.SKIN_TEMPERATURE -> {
                reading.skinTemperatureC?.let { latestSkinTemperatureC = it }
                reading.ambientTemperatureC?.let { latestAmbientTemperatureC = it }
                reading.temperatureC?.let {
                    latestTemperatureC = it
                    _temperatureC.value = it
                }
                maybePublishVitals()
            }
        }
    }

    private fun clearHeartRateDisplay() {
        latestHeartRate = null
        latestHrvMs = null
        _heartRate.value = 0
        maybePublishVitals(force = true)
    }

    private fun hasSuccessfulHeartRate(): Boolean =
        _heartRateStatus.value.isSuccessful && (latestHeartRate ?: 0) >= 30

    /**
     * Samsung HR status (-3 DETACHED) is more reliable than a single off-body sample.
     * Successful HR while worn overrides a false off-body reading.
     */
    private fun effectiveIsWearing(): Boolean {
        if (_heartRateStatus.value == HeartRateStatus.DETACHED) return false
        if (hasSuccessfulHeartRate()) return true
        if (confirmedOffWrist) return false
        return offBodyOnWrist ?: true
    }

    private fun refreshWearingState() {
        val wearing = effectiveIsWearing()
        if (isWearingNow != wearing) {
            isWearingNow = wearing
            motionHub.setWatchOnWrist(wearing)
            maybePublishVitals(force = true)
        }
    }

    private fun statusMessage(sensor: SensorType, reading: VitalReading): String = when (sensor) {
        SensorType.HEART_RATE -> when {
            !effectiveIsWearing() -> "Watch off wrist"
            reading.status == HeartRateStatus.DETACHED -> "Watch off wrist"
            reading.heartRateBpm != null -> "HR: ${reading.heartRateBpm} bpm"
            reading.status == HeartRateStatus.MOVEMENT -> "Hold still"
            else -> "Measuring HR…"
        }
        SensorType.SPO2 -> reading.spO2Percent?.let { "SpO₂: ${it.toInt()}%" } ?: "Measuring SpO₂…"
        SensorType.SKIN_TEMPERATURE -> reading.temperatureC?.let {
            "Temp: ${String.format(java.util.Locale.US, "%.1f", it)} °C"
        } ?: "Measuring temp…"
        SensorType.EDA -> reading.stressScore?.let { "Stress: ${it.toInt()}/100" } ?: "EDA…"
        SensorType.BIA -> reading.bodyFatPercent?.let {
            "Body fat: ${String.format(java.util.Locale.US, "%.1f", it)}%"
        } ?: "BIA — touch keys on watch"
        SensorType.ECG -> if (reading.ecgComplete) "ECG recorded" else "ECG — follow watch prompt"
    }

    private fun readSensor(intent: Intent): SensorType {
        val name = intent.getStringExtra(EXTRA_SENSOR)
        return runCatching { SensorType.valueOf(name ?: SensorType.HEART_RATE.name) }
            .getOrDefault(SensorType.HEART_RATE)
    }

    private fun normalizeGuid(value: String): String = try {
        UUID.fromString(value).toString()
    } catch (_: Exception) {
        val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        fun byteHex(b: Byte): String = "%02x".format(b.toInt() and 0xff)
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
            "Vitals Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Galaxy Watch vitals — HR, skin temperature, SpO₂"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun promoteToForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RPM – Vitals")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.rpm.watch.START_VITALS"
        const val ACTION_STOP = "com.rpm.watch.STOP_VITALS"
        const val EXTRA_SENSOR = "com.rpm.watch.EXTRA_SENSOR"

        fun startIntent(context: Context, sensor: SensorType) =
            Intent(context, VitalsMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SENSOR, sensor.name)
            }

        fun stopIntent(context: Context) =
            Intent(context, VitalsMonitorService::class.java).apply { action = ACTION_STOP }
    }
}
