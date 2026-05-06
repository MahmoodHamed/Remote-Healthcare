package com.rpm.watch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.watch.data.WatchDataStore
import com.rpm.watch.health.HrStatus
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

data class WatchUiState(
    val heartRate: Int = 0,
    val hrStatus: HrStatus = HrStatus.INITIAL,
    val serviceStatus: ServiceStatus = ServiceStatus.IDLE,
    val patientId: String = "",
    val isMonitoring: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: WatchDataStore
) : ViewModel() {

    // Exposed via service companion singleton
    private val _heartRate   = MutableStateFlow(0)
    private val _hrStatus    = MutableStateFlow(HrStatus.INITIAL)
    private val _svcStatus   = MutableStateFlow(ServiceStatus.IDLE)
    private val _patientId   = MutableStateFlow("")
    private val _isMonitoring = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")

    val uiState: StateFlow<WatchUiState> = combine(
        _heartRate,
        _hrStatus,
        _svcStatus,
        _isMonitoring,
        _errorMessage
    ) { hr, hrSt, svcSt, localMonitoring, errMsg ->
        WatchUiState(
            heartRate     = hr,
            hrStatus      = hrSt,
            serviceStatus = svcSt,
            patientId     = _patientId.value,
            isMonitoring  = localMonitoring || svcSt == ServiceStatus.MEASURING || svcSt == ServiceStatus.CONNECTING,
            errorMessage  = errMsg
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WatchUiState())

    init {
        viewModelScope.launch {
            dataStore.patientId.collect { id -> _patientId.value = id ?: "" }
        }
    }

    /** Update ViewModel state from the bound service (called by MainActivity). */
    fun attachService(service: HeartRateMonitorService) {
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
    }

    fun startMonitoring() {
        context.startForegroundService(HeartRateMonitorService.startIntent(context))
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        context.startService(HeartRateMonitorService.stopIntent(context))
        _isMonitoring.value = false
    }

    fun savePatientId(id: String) {
        viewModelScope.launch { dataStore.savePatientId(id) }
    }
}
