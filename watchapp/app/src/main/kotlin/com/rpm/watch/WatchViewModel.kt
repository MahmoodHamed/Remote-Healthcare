package com.rpm.watch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.health.AdvancedReading
import com.rpm.watch.health.HrStatus
import com.rpm.watch.mqtt.MqttConnectionState
import com.rpm.watch.mqtt.MqttManager
import com.rpm.watch.service.HeartRateMonitorService
import com.rpm.watch.service.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val heartRate: Int = 0,
    val hrStatus: HrStatus = HrStatus.INITIAL,
    val serviceStatus: ServiceStatus = ServiceStatus.IDLE,
    val patientId: String = "",
    val isMonitoring: Boolean = false,
    val errorMessage: String = "",
    val mqttConnectionState: MqttConnectionState = MqttConnectionState.DISCONNECTED,
    val sensors: AdvancedReading = AdvancedReading(),
    val measureMessage: String = "",
    val isMeasuringOnDemand: Boolean = false,
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: WatchDataStore,
    private val mqttManager: MqttManager,
) : ViewModel() {

    private val _heartRate   = MutableStateFlow(0)
    private val _hrStatus    = MutableStateFlow(HrStatus.INITIAL)
    private val _svcStatus   = MutableStateFlow(ServiceStatus.IDLE)
    private val _patientId   = MutableStateFlow("")
    private val _isMonitoring = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")
    private val _sensors = MutableStateFlow(AdvancedReading())
    private val _measureMessage = MutableStateFlow("")
    private val _isMeasuringOnDemand = MutableStateFlow(false)

    private var boundService: HeartRateMonitorService? = null

    val uiState: StateFlow<WatchUiState> = combine(
        _heartRate,
        _hrStatus,
        _svcStatus,
        _isMonitoring,
        _errorMessage,
        _sensors,
        _measureMessage,
        _isMeasuringOnDemand,
        mqttManager.connectionState,
    ) { values ->
        val hr = values[0] as Int
        val hrSt = values[1] as HrStatus
        val svcSt = values[2] as ServiceStatus
        val localMonitoring = values[3] as Boolean
        val errMsg = values[4] as String
        val sensors = values[5] as AdvancedReading
        val measureMsg = values[6] as String
        val measuring = values[7] as Boolean
        val mqttState = values[8] as MqttConnectionState
        WatchUiState(
            heartRate     = hr,
            hrStatus      = hrSt,
            serviceStatus = svcSt,
            patientId     = _patientId.value,
            isMonitoring  = localMonitoring || svcSt == ServiceStatus.MEASURING || svcSt == ServiceStatus.CONNECTING,
            errorMessage  = errMsg,
            mqttConnectionState = mqttState,
            sensors = sensors,
            measureMessage = measureMsg,
            isMeasuringOnDemand = measuring,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WatchUiState())

    private val _mqttHost = MutableStateFlow("")
    private val _mqttPort = MutableStateFlow(1883)

    val mqttHost: StateFlow<String> = _mqttHost
    val mqttPort: StateFlow<Int> = _mqttPort

    init {
        viewModelScope.launch {
            dataStore.patientId.collect { id -> _patientId.value = id ?: "" }
        }
        viewModelScope.launch {
            dataStore.mqttHost.collect { host -> _mqttHost.value = host }
        }
        viewModelScope.launch {
            dataStore.mqttPort.collect { port -> _mqttPort.value = port }
        }
    }

    fun attachService(service: HeartRateMonitorService) {
        boundService = service
        viewModelScope.launch {
            service.heartRate.collect { _heartRate.value = it }
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
            while (true) {
                boundService?.let { _sensors.value = it.sensorSnapshot() }
                delay(1000L)
            }
        }
    }

    fun startMonitoring() {
        context.startForegroundService(HeartRateMonitorService.startIntent(context))
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        context.startService(HeartRateMonitorService.stopIntent(context))
        _isMonitoring.value = false
    }

    fun measureSpO2() = runOnDemand("SpO₂") { svc, done ->
        svc.measureSpO2 { value ->
            _measureMessage.value = value?.let { "SpO₂: ${it.toInt()}%" } ?: "SpO₂ failed"
            done()
        }
    }

    fun measureEcg() = runOnDemand("ECG") { svc, done ->
        svc.measureEcg { hr, _ ->
            _measureMessage.value = hr?.let { "ECG avg HR: ${it.toInt()} bpm → server" } ?: "ECG failed"
            done()
        }
    }

    fun measureBodyFat() = runOnDemand("Body fat") { svc, done ->
        svc.measureBodyFat { value ->
            _measureMessage.value = value?.let { "Body fat: ${"%.1f".format(it)}%" } ?: "Body fat failed"
            done()
        }
    }

    private fun runOnDemand(label: String, block: (HeartRateMonitorService, () -> Unit) -> Unit) {
        val svc = boundService
        if (svc == null) {
            _measureMessage.value = "Start monitoring first"
            return
        }
        if (_isMeasuringOnDemand.value) return
        _isMeasuringOnDemand.value = true
        _measureMessage.value = "Measuring $label…"
        block(svc) { _isMeasuringOnDemand.value = false }
    }

    fun savePatientId(id: String) {
        viewModelScope.launch { dataStore.savePatientId(id) }
    }

    fun saveConfig(patientId: String, mqttHost: String, mqttPort: Int) {
        viewModelScope.launch {
            if (patientId.isNotBlank()) dataStore.savePatientId(patientId.trim().uppercase())
            if (mqttHost.isNotBlank()) dataStore.saveMqttConfig(mqttHost, mqttPort)
        }
    }
}
