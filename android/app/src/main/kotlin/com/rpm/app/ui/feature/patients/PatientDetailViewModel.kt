package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.PatientDetailDto
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.repository.ChatRepository
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.data.signalr.RealTimeVitals
import com.rpm.app.data.signalr.VitalsSignalRClient
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val isLoading: Boolean = false,
    val patient: PatientDetailDto? = null,
    val latestVitals: VitalRecordDto? = null,
    val realtimeVitals: RealTimeVitals? = null,
    val error: String? = null,
    val isOpeningChat: Boolean = false,
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repo: PatientRepository,
    private val chatRepo: ChatRepository,
    private val tokenStore: TokenDataStore,
    private val signalR: VitalsSignalRClient,
    savedStateHandle: SavedStateHandle,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val detail = repo.getPatientDetail(patientId)
            val vitals = repo.getLatestVitals(patientId)
            val patient = (detail as? Resource.Success)?.data
            _uiState.value = PatientDetailUiState(
                isLoading = false,
                patient = patient,
                latestVitals = (vitals as? Resource.Success)?.data,
                error = (detail as? Resource.Error)?.message
                    ?: (vitals as? Resource.Error)?.message.takeIf { patient == null },
            )
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            val connected = signalR.connect(patientId)
            if (!connected) {
                _uiState.value = _uiState.value.copy(
                    error = _uiState.value.error ?: "Live vitals unavailable (check login or network)",
                )
            }
            signalR.vitals.collect { v ->
                if (v.patientId == patientId) {
                    _uiState.value = _uiState.value.copy(realtimeVitals = v)
                }
            }
        }
    }

    fun openDoctorChat(onConversationReady: (conversationId: String) -> Unit) {
        viewModelScope.launch {
            val doctorId = tokenStore.userId.firstOrNull()
            val patient = _uiState.value.patient
            if (doctorId == null || patient == null) return@launch
            _uiState.value = _uiState.value.copy(isOpeningChat = true)
            when (
                val result = chatRepo.findOrCreateDoctorPatientConversation(
                    doctorId = doctorId,
                    patientId = patient.userId,
                    patientName = patient.fullName,
                )
            ) {
                is Resource.Success -> onConversationReady(result.data.id)
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    error = result.message,
                    isOpeningChat = false,
                )
                Resource.Loading -> {}
            }
            _uiState.value = _uiState.value.copy(isOpeningChat = false)
        }
    }

    override fun onCleared() {
        signalR.disconnect(patientId)
        super.onCleared()
    }
}
