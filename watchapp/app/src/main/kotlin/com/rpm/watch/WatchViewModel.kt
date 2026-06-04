package com.rpm.watch

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.SensorType
import com.rpm.watch.service.VitalsMonitorService
import com.rpm.watch.service.VitalsServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val heartRate: Int = 0,
    val temperatureC: Float? = null,
    val spO2Percent: Float? = null,
    val heartRateStatus: HeartRateStatus = HeartRateStatus.INITIAL,
    val serviceStatus: VitalsServiceStatus = VitalsServiceStatus.IDLE,
    val patientId: String = "",
    val mqttState: MqttConnectionState = MqttConnectionState.DISCONNECTED,
    val selectedSensor: SensorType = SensorType.HEART_RATE,
    val isMonitoring: Boolean = false,
    val errorMessage: String = "",
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: WatchDataStore,
    private val mqttManager: MqttManager,
) : ViewModel() {

    private val _heartRate = MutableStateFlow(0)
    private val _temperatureC = MutableStateFlow<Float?>(null)
    private val _spO2Percent = MutableStateFlow<Float?>(null)
    private val _heartRateStatus = MutableStateFlow(HeartRateStatus.INITIAL)
    private val _svcStatus = MutableStateFlow(VitalsServiceStatus.IDLE)
    private val _patientId = MutableStateFlow("")
    private val _selectedSensor = MutableStateFlow(SensorType.HEART_RATE)
    private val _isMonitoring = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")
    private val _mqttState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    private var serviceAttached = false

    var onRequestBindService: (() -> Unit)? = null
    var onRequestPermissions: ((SensorType, () -> Unit) -> Unit)? = null

    private var pendingStartSensor: SensorType? = null

    val uiState: StateFlow<WatchUiState> = combine(
        listOf(
            _heartRate,
            _temperatureC,
            _spO2Percent,
            _heartRateStatus,
            _svcStatus,
            _patientId,
            _mqttState,
            _selectedSensor,
            _isMonitoring,
            _errorMessage,
        ),
    ) { values ->
        val svcSt = values[4] as VitalsServiceStatus
        val localMonitoring = values[8] as Boolean
        WatchUiState(
            heartRate = values[0] as Int,
            temperatureC = values[1] as Float?,
            spO2Percent = values[2] as Float?,
            heartRateStatus = values[3] as HeartRateStatus,
            serviceStatus = svcSt,
            patientId = values[5] as String,
            mqttState = values[6] as MqttConnectionState,
            selectedSensor = values[7] as SensorType,
            isMonitoring = localMonitoring ||
                svcSt == VitalsServiceStatus.MEASURING ||
                svcSt == VitalsServiceStatus.CONNECTING,
            errorMessage = values[9] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WatchUiState())

    init {
        viewModelScope.launch {
            dataStore.patientId.collect { id -> _patientId.value = id ?: "" }
        }
        viewModelScope.launch {
            mqttManager.connectionState.collect { _mqttState.value = it }
        }
    }

    fun attachService(service: VitalsMonitorService) {
        if (serviceAttached) return
        serviceAttached = true
        _heartRate.value = service.heartRate.value
        _temperatureC.value = service.temperatureC.value
        _spO2Percent.value = service.spO2Percent.value
        _heartRateStatus.value = service.heartRateStatus.value
        _svcStatus.value = service.svcStatus.value
        _errorMessage.value = service.lastError.value
        viewModelScope.launch { service.heartRate.collect { _heartRate.value = it } }
        viewModelScope.launch { service.temperatureC.collect { _temperatureC.value = it } }
        viewModelScope.launch { service.spO2Percent.collect { _spO2Percent.value = it } }
        viewModelScope.launch { service.heartRateStatus.collect { _heartRateStatus.value = it } }
        viewModelScope.launch { service.svcStatus.collect { _svcStatus.value = it } }
        viewModelScope.launch { service.lastError.collect { _errorMessage.value = it } }
        viewModelScope.launch { service.mqttState.collect { _mqttState.value = it } }
    }

    fun selectSensor(sensor: SensorType) {
        _selectedSensor.value = sensor
    }

    fun startMonitoring(sensor: SensorType = _selectedSensor.value) {
        _selectedSensor.value = sensor
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
            _errorMessage.value = WatchPermissions.deniedMessage(_selectedSensor.value, missing)
            _svcStatus.value = VitalsServiceStatus.ERROR
        }
    }

    fun onPermissionsDenied(denied: Set<String>) {
        val sensor = pendingStartSensor ?: _selectedSensor.value
        _errorMessage.value = WatchPermissions.deniedMessage(sensor, denied)
        _svcStatus.value = VitalsServiceStatus.ERROR
        _isMonitoring.value = false
        pendingStartSensor = null
    }

    private fun startMonitoringInternal(sensor: SensorType) {
        try {
            _selectedSensor.value = sensor
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
        _temperatureC.value = null
        _spO2Percent.value = null
        _heartRateStatus.value = HeartRateStatus.INITIAL
    }

    fun savePatientId(id: String) {
        viewModelScope.launch { dataStore.savePatientId(id) }
    }
}
