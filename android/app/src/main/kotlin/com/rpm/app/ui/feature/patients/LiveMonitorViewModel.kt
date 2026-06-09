package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.data.signalr.RealTimeVitals
import com.rpm.app.data.signalr.VitalsSignalRClient
import com.rpm.app.data.signalr.mergeWith
import com.rpm.app.data.signalr.toRealTime
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LiveHistoryEntry(
    val time: String,
    val hr: String,
    val spo2: String,
    val skinTemp: String,
    val ambientTemp: String,
    val hrv: String,
    val stress: String,
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
        startSignalR()
        loadLatestFromApi()
        startRestPolling()
    }

    private fun loadPatientName() {
        viewModelScope.launch {
            when (val r = repo.getPatientDetail(patientId)) {
                is Resource.Success -> _state.update { it.copy(patientName = r.data.fullName) }
                else -> {}
            }
        }
    }

    private fun loadLatestFromApi() {
        viewModelScope.launch {
            when (val result = repo.getLatestVitals(patientId)) {
                is Resource.Success -> {
                    val latest = result.data
                    if (latest != null) {
                        applyVitals(latest.toRealTime(), fromSignalR = false)
                    } else {
                        loadMostRecentFromHistory()
                    }
                }
                is Resource.Error -> loadMostRecentFromHistory()
                else -> {}
            }
        }
    }

    private suspend fun loadMostRecentFromHistory() {
        when (val result = repo.getVitals(patientId, page = 1)) {
            is Resource.Success -> {
                result.data.items.firstOrNull()?.let {
                    applyVitals(it.toRealTime(), fromSignalR = false)
                }
            }
            else -> {}
        }
    }

    private fun startSignalR() {
        viewModelScope.launch {
            signalR.vitals.collect { incoming ->
                applyVitals(incoming, fromSignalR = true)
            }
        }
        viewModelScope.launch {
            val connected = signalR.connect(patientId)
            _state.update {
                it.copy(connectionStatus = if (connected) ConnectionStatus.Live else ConnectionStatus.Offline)
            }
        }
    }

    private fun startRestPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                when (val result = repo.getLatestVitals(patientId)) {
                    is Resource.Success -> result.data?.let {
                        applyVitals(it.toRealTime(), fromSignalR = false)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun applyVitals(incoming: RealTimeVitals, fromSignalR: Boolean) {
        val merged = incoming.mergeWith(_state.value.vitals)
        val entry = LiveHistoryEntry(
            time         = timeFmt.format(Instant.now()),
            hr           = merged.heartRateBpm?.toInt()?.toString() ?: "--",
            spo2         = merged.spO2Percent?.let { "%.1f".format(it) } ?: "--",
            skinTemp     = merged.skinTemperatureC?.let { "%.1f".format(it) } ?: "--",
            ambientTemp  = merged.ambientTemperatureC?.let { "%.1f".format(it) } ?: "--",
            hrv          = merged.hrvMs?.toInt()?.toString() ?: "--",
            stress       = merged.stressScore?.toInt()?.toString() ?: "--",
            steps        = merged.stepsCount?.toString() ?: "--",
            fallDetected = merged.fallDetected,
            wearing      = SupportedVitals.isWearing(merged),
        )
        _state.update { s ->
            s.copy(
                connectionStatus = if (fromSignalR) ConnectionStatus.Live else s.connectionStatus,
                vitals = merged,
                history = (listOf(entry) + s.history).take(20),
            )
        }
    }

    override fun onCleared() {
        signalR.disconnect(patientId)
        super.onCleared()
    }
}
