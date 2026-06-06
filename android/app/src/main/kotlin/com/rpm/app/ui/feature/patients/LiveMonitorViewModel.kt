package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.data.signalr.RealTimeVitals
import com.rpm.app.data.signalr.VitalsSignalRClient
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LiveHistoryEntry(
    val time: String,
    val hr: String,
    val spo2: String,
    val tempC: String,
    val skinTemp: String,
    val hrv: String,
    val steps: String,
    val fallDetected: Boolean,
    val wearing: Boolean,
)

enum class ConnectionStatus { Connecting, Live, Offline }

data class LiveMonitorUiState(
    val patientName: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connecting,
    val vitals: RealTimeVitals? = null,
    val history: List<LiveHistoryEntry> = emptyList(),
)

@HiltViewModel
class LiveMonitorViewModel @Inject constructor(
    private val repo: PatientRepository,
    private val signalR: VitalsSignalRClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _state = MutableStateFlow(LiveMonitorUiState())
    val state: StateFlow<LiveMonitorUiState> = _state.asStateFlow()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    init {
        loadPatientName()
        subscribeRealtime()
    }

    private fun loadPatientName() {
        viewModelScope.launch {
            when (val r = repo.getPatientDetail(patientId)) {
                is Resource.Success -> _state.update { it.copy(patientName = r.data.fullName) }
                else -> {}
            }
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            val connected = signalR.connect(patientId)
            _state.update { it.copy(connectionStatus = if (connected) ConnectionStatus.Live else ConnectionStatus.Offline) }
            if (connected) {
                signalR.vitals.collect { v ->
                    if (v.patientId != patientId) return@collect
                    val entry = LiveHistoryEntry(
                        time        = timeFmt.format(Instant.now()),
                        hr          = v.heartRateBpm?.toInt()?.toString() ?: "--",
                        spo2        = v.spO2Percent?.let { "%.1f".format(it) } ?: "--",
                        tempC       = v.temperatureC?.let { "%.1f".format(it) } ?: "--",
                        skinTemp    = v.skinTemperatureC?.let { "%.1f".format(it) } ?: "--",
                        hrv         = v.hrvMs?.toInt()?.toString() ?: "--",
                        steps       = v.stepsCount?.toString() ?: "--",
                        fallDetected = v.fallDetected,
                        wearing     = v.isWearing,
                    )
                    _state.update { s ->
                        s.copy(
                            connectionStatus = ConnectionStatus.Live,
                            vitals  = v,
                            history = (listOf(entry) + s.history).take(12),
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        // Don't disconnect — PatientDetailViewModel (still on the back stack)
        // owns the SignalR connection lifecycle for this patient.
        super.onCleared()
    }
}
