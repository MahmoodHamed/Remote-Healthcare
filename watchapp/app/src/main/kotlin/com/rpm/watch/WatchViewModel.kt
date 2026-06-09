package com.rpm.watch

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.watch.BuildConfig
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.SamsungHealthLauncher
import com.rpm.watch.sensor.SensorType
import com.rpm.watch.service.VitalsMonitorService
import com.rpm.watch.service.VitalsServiceStatus
import com.rpm.watch.ui.WatchViewMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val heartRate: Int = 0,
    val skinTemperatureC: Float? = null,
    val ambientTemperatureC: Float? = null,
    val spO2Percent: Float? = null,
    val hrvMs: Float? = null,
    val stressScore: Float? = null,
    val stepsCount: Int? = null,
    val caloriesBurned: Float? = null,
    val fallDetected: Boolean = false,
    val isWearing: Boolean = true,
    val bodyFatPercent: Float? = null,
    val heartRateStatus: HeartRateStatus = HeartRateStatus.INITIAL,
    val serviceStatus: VitalsServiceStatus = VitalsServiceStatus.IDLE,
    val patientId: String = "",
    val mqttState: MqttConnectionState = MqttConnectionState.DISCONNECTED,
    val selectedMetric: WatchViewMetric = WatchViewMetric.HEART_RATE,
    val isMonitoring: Boolean = false,
    val errorMessage: String = "",
    val ecgMeasuring: Boolean = false,
    val biaMeasuring: Boolean = false,
    val ecgAvgHeartRateBpm: Float? = null,
    val samsungHealthMonitorInstalled: Boolean = false,
)

data class WatchSettingsState(
    val patientId: String = "",
    val mqttHost: String = com.rpm.watch.BuildConfig.MQTT_HOST,
    val mqttPort: String = com.rpm.watch.BuildConfig.MQTT_PORT.toString(),
    val wasMonitoring: Boolean = false,
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: WatchDataStore,
    private val mqttManager: MqttManager,
) : ViewModel() {

    private val _heartRate = MutableStateFlow(0)
    private val _skinTemperatureC = MutableStateFlow<Float?>(null)
    private val _ambientTemperatureC = MutableStateFlow<Float?>(null)
    private val _spO2Percent = MutableStateFlow<Float?>(null)
    private val _hrvMs = MutableStateFlow<Float?>(null)
    private val _stressScore = MutableStateFlow<Float?>(null)
    private val _stepsCount = MutableStateFlow<Int?>(null)
    private val _caloriesBurned = MutableStateFlow<Float?>(null)
    private val _fallDetected = MutableStateFlow(false)
    private val _isWearing = MutableStateFlow(true)
    private val _bodyFatPercent = MutableStateFlow<Float?>(null)
    private val _heartRateStatus = MutableStateFlow(HeartRateStatus.INITIAL)
    private val _svcStatus = MutableStateFlow(VitalsServiceStatus.IDLE)
    private val _patientId = MutableStateFlow("")
    private val _selectedMetric = MutableStateFlow(WatchViewMetric.HEART_RATE)
    private val _isMonitoring = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")
    private val _mqttState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    private val _ecgMeasuring = MutableStateFlow(false)
    private val _biaMeasuring = MutableStateFlow(false)
    private val _ecgAvgHeartRateBpm = MutableStateFlow<Float?>(null)
    private val _samsungHealthInstalled = MutableStateFlow(
        SamsungHealthLauncher.isHealthMonitorInstalled(context),
    )
    private val _settingsState = MutableStateFlow(WatchSettingsState())
    val settingsState: StateFlow<WatchSettingsState> = _settingsState.asStateFlow()
    private var serviceAttached = false

    var onRequestBindService: (() -> Unit)? = null
    var onRequestPermissions: ((SensorType, () -> Unit) -> Unit)? = null
    var onRequestEcg: (() -> Unit)? = null
    var onRequestBia: (() -> Unit)? = null

    private var pendingStartSensor: SensorType? = null

    val uiState: StateFlow<WatchUiState> = combine(
        listOf(
            _heartRate,
            _skinTemperatureC,
            _ambientTemperatureC,
            _spO2Percent,
            _hrvMs,
            _stressScore,
            _stepsCount,
            _caloriesBurned,
            _fallDetected,
            _isWearing,
            _bodyFatPercent,
            _heartRateStatus,
            _svcStatus,
            _patientId,
            _mqttState,
            _selectedMetric,
            _isMonitoring,
            _errorMessage,
            _ecgMeasuring,
            _biaMeasuring,
            _ecgAvgHeartRateBpm,
            _samsungHealthInstalled,
        ),
    ) { values ->
        val svcSt = values[12] as VitalsServiceStatus
        val localMonitoring = values[16] as Boolean
        WatchUiState(
            heartRate = values[0] as Int,
            skinTemperatureC = values[1] as Float?,
            ambientTemperatureC = values[2] as Float?,
            spO2Percent = values[3] as Float?,
            hrvMs = values[4] as Float?,
            stressScore = values[5] as Float?,
            stepsCount = values[6] as Int?,
            caloriesBurned = values[7] as Float?,
            fallDetected = values[8] as Boolean,
            isWearing = values[9] as Boolean,
            bodyFatPercent = values[10] as Float?,
            heartRateStatus = values[11] as HeartRateStatus,
            serviceStatus = svcSt,
            patientId = values[13] as String,
            mqttState = values[14] as MqttConnectionState,
            selectedMetric = values[15] as WatchViewMetric,
            isMonitoring = localMonitoring ||
                svcSt == VitalsServiceStatus.MEASURING ||
                svcSt == VitalsServiceStatus.CONNECTING,
            errorMessage = values[17] as String,
            ecgMeasuring = values[18] as Boolean,
            biaMeasuring = values[19] as Boolean,
            ecgAvgHeartRateBpm = values[20] as Float?,
            samsungHealthMonitorInstalled = values[21] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WatchUiState())

    init {
        viewModelScope.launch {
            dataStore.patientId.collect { id ->
                _patientId.value = id?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_PATIENT_ID
            }
        }
        viewModelScope.launch {
            mqttManager.connectionState.collect { _mqttState.value = it }
        }
        viewModelScope.launch {
            combine(dataStore.patientId, dataStore.mqttHost, dataStore.mqttPort) { pid, host, port ->
                Triple(pid, host, port)
            }.collect { (pid, host, port) ->
                _settingsState.value = _settingsState.value.copy(
                    patientId = pid?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_PATIENT_ID,
                    mqttHost = host,
                    mqttPort = port.toString(),
                )
            }
        }
    }

    fun saveSettings(patientId: String, mqttHost: String, mqttPort: Int) {
        viewModelScope.launch {
            val wasMonitoring = _isMonitoring.value
            dataStore.savePatientId(patientId.trim())
            dataStore.saveMqttConfig(mqttHost.trim(), mqttPort.coerceIn(1, 65535))
            _settingsState.value = _settingsState.value.copy(
                patientId = patientId.trim(),
                mqttHost = mqttHost.trim(),
                mqttPort = mqttPort.toString(),
                wasMonitoring = wasMonitoring,
            )
            if (wasMonitoring) {
                stopMonitoring()
            }
        }
    }

    fun attachService(service: VitalsMonitorService) {
        if (serviceAttached) return
        serviceAttached = true
        syncFromService(service)
        viewModelScope.launch { service.heartRate.collect { _heartRate.value = it } }
        viewModelScope.launch { service.skinTemperatureC.collect { _skinTemperatureC.value = it } }
        viewModelScope.launch { service.ambientTemperatureC.collect { _ambientTemperatureC.value = it } }
        viewModelScope.launch { service.spO2Percent.collect { _spO2Percent.value = it } }
        viewModelScope.launch { service.hrvMs.collect { _hrvMs.value = it } }
        viewModelScope.launch { service.stressScore.collect { _stressScore.value = it } }
        viewModelScope.launch { service.stepsCount.collect { _stepsCount.value = it } }
        viewModelScope.launch { service.caloriesBurned.collect { _caloriesBurned.value = it } }
        viewModelScope.launch { service.fallDetected.collect { _fallDetected.value = it } }
        viewModelScope.launch { service.isWearing.collect { _isWearing.value = it } }
        viewModelScope.launch { service.bodyFatPercent.collect { _bodyFatPercent.value = it } }
        viewModelScope.launch { service.heartRateStatus.collect { _heartRateStatus.value = it } }
        viewModelScope.launch { service.svcStatus.collect { _svcStatus.value = it } }
        viewModelScope.launch { service.lastError.collect { _errorMessage.value = it } }
        viewModelScope.launch { service.mqttState.collect { _mqttState.value = it } }
        viewModelScope.launch { service.ecgMeasuring.collect { _ecgMeasuring.value = it } }
        viewModelScope.launch { service.biaMeasuring.collect { _biaMeasuring.value = it } }
        viewModelScope.launch { service.ecgAvgHeartRateBpm.collect { _ecgAvgHeartRateBpm.value = it } }
    }

    private fun syncFromService(service: VitalsMonitorService) {
        _heartRate.value = service.heartRate.value
        _skinTemperatureC.value = service.skinTemperatureC.value
        _ambientTemperatureC.value = service.ambientTemperatureC.value
        _spO2Percent.value = service.spO2Percent.value
        _hrvMs.value = service.hrvMs.value
        _stressScore.value = service.stressScore.value
        _stepsCount.value = service.stepsCount.value
        _caloriesBurned.value = service.caloriesBurned.value
        _fallDetected.value = service.fallDetected.value
        _isWearing.value = service.isWearing.value
        _bodyFatPercent.value = service.bodyFatPercent.value
        _heartRateStatus.value = service.heartRateStatus.value
        _svcStatus.value = service.svcStatus.value
        _errorMessage.value = service.lastError.value
        _ecgMeasuring.value = service.ecgMeasuring.value
        _biaMeasuring.value = service.biaMeasuring.value
        _ecgAvgHeartRateBpm.value = service.ecgAvgHeartRateBpm.value
    }

    fun startEcgMeasurement() {
        if (!_isMonitoring.value) startMonitoring()
        onRequestBindService?.invoke()
        onRequestEcg?.invoke()
    }

    fun startBodyFatMeasurement() {
        if (!_isMonitoring.value) startMonitoring()
        onRequestBindService?.invoke()
        onRequestBia?.invoke()
    }

    fun openSamsungHealthMonitor(): Boolean {
        _samsungHealthInstalled.value = SamsungHealthLauncher.isHealthMonitorInstalled(context)
        val opened = SamsungHealthLauncher.openHealthMonitor(context)
        if (!opened) {
            _errorMessage.value = "Install Samsung Health Monitor on watch"
            _svcStatus.value = VitalsServiceStatus.ERROR
        }
        return opened
    }

    fun selectMetric(metric: WatchViewMetric) {
        _selectedMetric.value = metric
    }

    fun startMonitoring(sensor: SensorType = SensorType.HEART_RATE) {
        val missing = WatchPermissions.missingForAllVitals(context)
        if (missing.isNotEmpty()) {
            pendingStartSensor = sensor
            _errorMessage.value = WatchPermissions.deniedMessage(sensor, missing)
            _svcStatus.value = VitalsServiceStatus.ERROR
            onRequestPermissions?.invoke(sensor) {
                val pending = pendingStartSensor
                pendingStartSensor = null
                if (pending != null && WatchPermissions.hasAllForVitals(context)) {
                    startMonitoringInternal(pending)
                }
            } ?: Log.w("WatchViewModel", "Missing permissions: $missing")
            return
        }
        startMonitoringInternal(sensor)
    }

    fun onPermissionsGranted() {
        if (_errorMessage.value.isNotBlank()) {
            _errorMessage.value = ""
            _svcStatus.value = VitalsServiceStatus.IDLE
        }
    }

    fun showPermissionReminder(missing: List<String>) {
        if (!_isMonitoring.value && missing.isNotEmpty()) {
            _errorMessage.value = WatchPermissions.deniedMessage(SensorType.HEART_RATE, missing)
            _svcStatus.value = VitalsServiceStatus.ERROR
        }
    }

    fun onPermissionsDenied(denied: Set<String>) {
        val sensor = pendingStartSensor ?: SensorType.HEART_RATE
        _errorMessage.value = WatchPermissions.deniedMessage(sensor, denied)
        _svcStatus.value = VitalsServiceStatus.ERROR
        _isMonitoring.value = false
        pendingStartSensor = null
    }

    private fun startMonitoringInternal(sensor: SensorType) {
        try {
            resetDisplayedVitals()
            onRequestBindService?.invoke()
            context.startForegroundService(VitalsMonitorService.startIntent(context, sensor))
            _isMonitoring.value = true
            _errorMessage.value = ""
            _svcStatus.value = VitalsServiceStatus.CONNECTING
        } catch (e: Exception) {
            Log.e("WatchViewModel", "Failed to start monitoring", e)
            _isMonitoring.value = false
            _errorMessage.value = e.message ?: "Could not start monitoring"
            _svcStatus.value = VitalsServiceStatus.ERROR
        }
    }

    fun stopMonitoring() {
        context.startService(VitalsMonitorService.stopIntent(context))
        _isMonitoring.value = false
        serviceAttached = false
        resetDisplayedVitals()
        _svcStatus.value = VitalsServiceStatus.IDLE
    }

    private fun resetDisplayedVitals() {
        _heartRate.value = 0
        _skinTemperatureC.value = null
        _ambientTemperatureC.value = null
        _spO2Percent.value = null
        _hrvMs.value = null
        _stressScore.value = null
        _stepsCount.value = null
        _caloriesBurned.value = null
        _fallDetected.value = false
        _isWearing.value = true
        _bodyFatPercent.value = null
        _heartRateStatus.value = HeartRateStatus.INITIAL
        _biaMeasuring.value = false
    }

    fun savePatientId(id: String) {
        viewModelScope.launch { dataStore.savePatientId(id) }
    }
}
