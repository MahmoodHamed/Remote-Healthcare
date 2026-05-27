package com.rpm.watch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.health.HrStatus
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.service.HeartRateMonitorService
import com.rpm.watch.service.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MonitoringMode {
    HEART_RATE,
    TEMPERATURE,
    SPO2
}

data class WatchUiState(
    val heartRate: Int = 0,
    val temperatureC: Float? = null,
    val spO2Percent: Float? = null,
    val hrStatus: HrStatus = HrStatus.INITIAL,
    val serviceStatus: ServiceStatus = ServiceStatus.IDLE,
    val patientId: String = "",
    val mqttState: MqttConnectionState = MqttConnectionState.DISCONNECTED,
    val selectedMode: MonitoringMode = MonitoringMode.HEART_RATE,
    val isMonitoring: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: WatchDataStore,
    private val mqttManager: MqttManager
) : ViewModel() {

    // Exposed via service companion singleton
    private val _heartRate = MutableStateFlow(0)
    private val _temperatureC = MutableStateFlow<Float?>(null)
    private val _spO2Percent = MutableStateFlow<Float?>(null)
    private val _hrStatus = MutableStateFlow(HrStatus.INITIAL)
    private val _svcStatus = MutableStateFlow(ServiceStatus.IDLE)
    private val _patientId = MutableStateFlow("")
    private val _selectedMode = MutableStateFlow(MonitoringMode.HEART_RATE)
    private val _isMonitoring = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")
    private val _mqttState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    private var serviceAttached = false

    /** Set from [MainActivity] so monitoring can bind the foreground service. */
    var onRequestBindService: (() -> Unit)? = null

    val uiState: StateFlow<WatchUiState> = combine(
        listOf(
            _heartRate,
            _temperatureC,
            _spO2Percent,
            _hrStatus,
            _svcStatus,
            _patientId,
            _mqttState,
            _selectedMode,
            _isMonitoring,
            _errorMessage
        )
    ) { values ->
        val hr = values[0] as Int
        val temp = values[1] as Float?
        val spo2 = values[2] as Float?
        val hrSt = values[3] as HrStatus
        val svcSt = values[4] as ServiceStatus
        val pid = values[5] as String
        val mqtt = values[6] as MqttConnectionState
        val mode = values[7] as MonitoringMode
        val localMonitoring = values[8] as Boolean
        val errMsg = values[9] as String

        WatchUiState(
            heartRate     = hr,
            temperatureC  = temp,
            spO2Percent   = spo2,
            hrStatus      = hrSt,
            serviceStatus = svcSt,
            patientId     = pid,
            mqttState     = mqtt,
            selectedMode  = mode,
            isMonitoring  = localMonitoring || svcSt == ServiceStatus.MEASURING || svcSt == ServiceStatus.CONNECTING,
            errorMessage  = errMsg
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

    /** Update ViewModel state from the bound service (called by MainActivity). */
    fun attachService(service: HeartRateMonitorService) {
        if (serviceAttached) return
        serviceAttached = true
        viewModelScope.launch {
            service.heartRate.collect { _heartRate.value = it }
        }
        viewModelScope.launch {
            service.temperatureC.collect { _temperatureC.value = it }
        }
        viewModelScope.launch {
            service.spO2Percent.collect { _spO2Percent.value = it }
        }
        viewModelScope.launch {
            service.hrStatus.collect { _hrStatus.value = it }
        }
        viewModelScope.launch {
            service.svcStatus.collect { _svcStatus.value = it }
        }
        viewModelScope.launch {
            service.lastError.collect { _errorMessage.value = it }
        }
        viewModelScope.launch {
            service.mqttState.collect { _mqttState.value = it }
        }
    }

    fun selectMode(mode: MonitoringMode) {
        _selectedMode.value = mode
    }

    fun startMonitoring(mode: MonitoringMode = _selectedMode.value) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _errorMessage.value = "Allow body sensor permission first"
            _svcStatus.value = ServiceStatus.ERROR
            return
        }
        try {
            _selectedMode.value = mode
            context.startForegroundService(HeartRateMonitorService.startIntent(context, mode))
            _isMonitoring.value = true
            _errorMessage.value = ""
            _svcStatus.value = ServiceStatus.CONNECTING
            onRequestBindService?.invoke()
        } catch (e: Exception) {
            Log.e("WatchViewModel", "Failed to start monitoring", e)
            _isMonitoring.value = false
            _errorMessage.value = e.message ?: "Could not start monitoring"
            _svcStatus.value = ServiceStatus.ERROR
        }
    }

    fun stopMonitoring() {
        context.startService(HeartRateMonitorService.stopIntent(context))
        _isMonitoring.value = false
        serviceAttached = false
        _heartRate.value = 0
        _hrStatus.value = HrStatus.INITIAL
        _svcStatus.value = ServiceStatus.IDLE
    }

    fun savePatientId(id: String) {
        viewModelScope.launch { dataStore.savePatientId(id) }
    }
}
