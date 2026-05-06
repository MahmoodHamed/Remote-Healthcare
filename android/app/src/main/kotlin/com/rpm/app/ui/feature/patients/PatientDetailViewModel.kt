package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.PatientDetailDto
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.data.signalr.RealTimeVitals
import com.rpm.app.data.signalr.VitalsSignalRClient
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val isLoading: Boolean = false,
    val patient: PatientDetailDto? = null,
    val latestVitals: VitalRecordDto? = null,
    val realtimeVitals: RealTimeVitals? = null,
    val error: String? = null
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repo: PatientRepository,
    private val signalR: VitalsSignalRClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _uiState = MutableStateFlow(PatientDetailUiState(isLoading = true))
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    init {
        loadPatient()
        subscribeRealtime()
    }

    private fun loadPatient() {
        viewModelScope.launch {
            val detail = repo.getPatientDetail(patientId)
            val vitals = repo.getLatestVitals(patientId)
            _uiState.value = PatientDetailUiState(
                patient = (detail as? Resource.Success)?.data,
                latestVitals = (vitals as? Resource.Success)?.data,
                error = (detail as? Resource.Error)?.message
            )
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            signalR.connect(patientId)
            signalR.vitals.collect { v ->
                if (v.patientId == patientId) {
                    _uiState.value = _uiState.value.copy(realtimeVitals = v)
                }
            }
        }
    }

    override fun onCleared() {
        signalR.disconnect(patientId)
        super.onCleared()
    }
}
