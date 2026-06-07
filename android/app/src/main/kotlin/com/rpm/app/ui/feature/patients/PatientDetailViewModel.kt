package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.PatientDetailDto
import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.remote.dto.forDisplay
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.data.signalr.VitalsSignalRClient
import com.rpm.app.domain.model.Resource
import com.rpm.app.util.ShortIdNormalizer
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
    val realtimeVitals: VitalRecordDto? = null,
    /** The UUID actually used to subscribe to the vitals hub (may differ from userId). */
    val streamingPatientId: String = "",
    val watchShortId: String = "",
    val error: String? = null
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repo: PatientRepository,
    private val signalR: VitalsSignalRClient,
    private val tokenStore: TokenDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Raw patient/user ID from the navigation argument (the real account UUID). */
    private val navPatientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _uiState = MutableStateFlow(PatientDetailUiState(isLoading = true))
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shortId = tokenStore.getWatchShortId() ?: ""
            val streamingId = resolveStreamingId(shortId)
            _uiState.value = _uiState.value.copy(watchShortId = shortId, streamingPatientId = streamingId)
            loadPatient(streamingId)
            subscribeRealtime(streamingId)
        }
    }

    /**
     * Resolve the UUID we actually subscribe / query vitals for.
     * Priority: short-ID-derived UUID > real user UUID.
     */
    private fun resolveStreamingId(shortId: String): String {
        if (ShortIdNormalizer.isValidShortId(shortId)) {
            val normalized = ShortIdNormalizer.normalize(shortId)
            if (normalized != null) return normalized
        }
        return navPatientId
    }

    private fun loadPatient(streamingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val detail = repo.getPatientDetail(navPatientId)
            val vitals = repo.getLatestVitals(streamingId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                patient = (detail as? Resource.Success)?.data,
                latestVitals = (vitals as? Resource.Success)?.data?.forDisplay(),
                error = when {
                    detail is Resource.Error -> detail.message
                    else -> null
                }
            )
        }
    }

    private fun subscribeRealtime(streamingId: String) {
        viewModelScope.launch {
            try {
                signalR.connect(streamingId)
            } catch (e: Exception) {
                return@launch
            }
            signalR.vitals.collect { v ->
                if (v.patientId.equals(streamingId, ignoreCase = true)) {
                    _uiState.value = _uiState.value.copy(realtimeVitals = v.forDisplay())
                }
            }
        }
    }

    fun saveWatchShortId(shortId: String) {
        val trimmed = shortId.trim().uppercase()
        if (!ShortIdNormalizer.isValidShortId(trimmed) && trimmed.isNotEmpty()) return
        viewModelScope.launch {
            tokenStore.saveWatchShortId(trimmed)
            val streamingId = resolveStreamingId(trimmed)
            _uiState.value = _uiState.value.copy(watchShortId = trimmed, streamingPatientId = streamingId)
            signalR.disconnect(navPatientId)
            loadPatient(streamingId)
            subscribeRealtime(streamingId)
        }
    }

    fun refresh() {
        val streamingId = _uiState.value.streamingPatientId.ifBlank { navPatientId }
        loadPatient(streamingId)
    }

    override fun onCleared() {
        signalR.disconnect(_uiState.value.streamingPatientId)
        super.onCleared()
    }
}
