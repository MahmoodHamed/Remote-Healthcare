package com.rpm.app.ui.feature.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.PatientDetailDto
import com.rpm.app.data.remote.dto.SetWatchShortIdRequest
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
            // 1. Use locally stored short ID first for immediate streaming
            val localShortId = tokenStore.getWatchShortId() ?: ""
            val initialStreamingId = resolveStreamingId(localShortId)
            _uiState.value = _uiState.value.copy(
                watchShortId = localShortId,
                streamingPatientId = initialStreamingId
            )

            // 2. Fetch patient profile — it may contain a watchShortId stored server-side
            val detailResult = repo.getPatientDetail(navPatientId)
            val profileShortId = (detailResult as? Resource.Success)?.data?.watchShortId ?: ""

            // 3. Server-side short ID wins if it's valid; sync it locally
            val resolvedShortId = when {
                ShortIdNormalizer.isValidShortId(profileShortId) -> {
                    tokenStore.saveWatchShortId(profileShortId)
                    profileShortId
                }
                ShortIdNormalizer.isValidShortId(localShortId) -> localShortId
                else -> ""
            }
            val streamingId = resolveStreamingId(resolvedShortId)

            _uiState.value = _uiState.value.copy(
                patient = (detailResult as? Resource.Success)?.data,
                watchShortId = resolvedShortId,
                streamingPatientId = streamingId,
                error = (detailResult as? Resource.Error)?.message
            )

            // 4. Load vitals using the resolved streaming ID
            loadVitals(streamingId)
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

    private fun loadVitals(streamingId: String) {
        viewModelScope.launch {
            val vitals = repo.getLatestVitals(streamingId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                latestVitals = (vitals as? Resource.Success)?.data?.forDisplay()
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
            // Persist to backend so web and other devices sync automatically
            repo.setWatchShortId(navPatientId, trimmed.ifEmpty { null })
            val streamingId = resolveStreamingId(trimmed)
            _uiState.value = _uiState.value.copy(watchShortId = trimmed, streamingPatientId = streamingId)
            signalR.disconnect(_uiState.value.streamingPatientId)
            loadVitals(streamingId)
            subscribeRealtime(streamingId)
        }
    }

    fun refresh() {
        val streamingId = _uiState.value.streamingPatientId.ifBlank { navPatientId }
        loadVitals(streamingId)
    }

    override fun onCleared() {
        signalR.disconnect(_uiState.value.streamingPatientId)
        super.onCleared()
    }
}
