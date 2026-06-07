package com.rpm.app.ui.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.BuildConfig
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.repository.DeviceRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceManagementUiState(
    val devices: List<DeviceDto> = emptyList(),
    val shortCodeInput: String = BuildConfig.DEFAULT_PATIENT_ID,
    val streamingPatientId: String = "",
    val mqttHost: String = BuildConfig.MQTT_HOST,
    val mqttPort: Int = BuildConfig.MQTT_PORT,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedLocally: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val renameSuccess: Boolean = false,
)

@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val repo: DeviceRepository,
    private val tokenStore: TokenDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceManagementUiState())
    val state: StateFlow<DeviceManagementUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val fallbackUserId = tokenStore.userId.firstOrNull()?.trim().orEmpty()
            var err: String? = null

            val devices = when (val devResult = repo.getMyDevices()) {
                is Resource.Success -> devResult.data
                is Resource.Error -> {
                    if (devResult.httpCode != 404) err = devResult.message
                    emptyList()
                }
                else -> emptyList()
            }

            var shortCode = BuildConfig.DEFAULT_PATIENT_ID
            var streamingId = fallbackUserId
            var mqttHost = BuildConfig.MQTT_HOST
            var mqttPort = BuildConfig.MQTT_PORT
            var localOnly = false

            when (val pairResult = repo.getDevicePairingInfo()) {
                is Resource.Success -> {
                    val info = pairResult.data.info
                    shortCode = info.patientId.ifBlank { BuildConfig.DEFAULT_PATIENT_ID }
                    streamingId = info.streamingPatientId.ifBlank { fallbackUserId }
                    mqttHost = info.mqttHost.ifBlank { BuildConfig.MQTT_HOST }
                    mqttPort = info.mqttPort.takeIf { it > 0 } ?: BuildConfig.MQTT_PORT
                    localOnly = pairResult.data.savedLocally
                }
                is Resource.Error -> err = pairResult.message
                else -> {}
            }

            _state.update {
                it.copy(
                    devices = devices,
                    shortCodeInput = shortCode,
                    streamingPatientId = streamingId,
                    mqttHost = mqttHost,
                    mqttPort = mqttPort,
                    savedLocally = localOnly,
                    isLoading = false,
                    error = err,
                )
            }
        }
    }

    fun updateShortCode(value: String) {
        val filtered = value.filter { it.isLetterOrDigit() }.take(6).uppercase()
        _state.update { it.copy(shortCodeInput = filtered, saveSuccess = false) }
    }

    fun savePairing() {
        val code = _state.value.shortCodeInput.trim()
        if (code.length != 6) {
            _state.update { it.copy(error = "Patient short ID must be exactly 6 letters or digits (e.g. ABC123).") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = repo.saveDevicePairingInfo(code)) {
                is Resource.Success -> {
                    val info = result.data.info
                    _state.update {
                        it.copy(
                            shortCodeInput = info.patientId.ifBlank { code },
                            streamingPatientId = info.streamingPatientId.ifBlank { it.streamingPatientId },
                            mqttHost = info.mqttHost.ifBlank { BuildConfig.MQTT_HOST },
                            mqttPort = info.mqttPort.takeIf { p -> p > 0 } ?: BuildConfig.MQTT_PORT,
                            savedLocally = result.data.savedLocally,
                            isSaving = false,
                            saveSuccess = true,
                        )
                    }
                    // Refresh device list after saving pairing
                    when (val devResult = repo.getMyDevices()) {
                        is Resource.Success -> _state.update { s -> s.copy(devices = devResult.data) }
                        else -> {}
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false, error = result.message) }
                }
                Resource.Loading -> {}
            }
        }
    }

    fun renameDevice(id: String, newName: String) {
        viewModelScope.launch {
            val result = repo.renameDevice(id, newName)
            if (result is Resource.Error) {
                _state.update { it.copy(error = result.message) }
            } else {
                _state.update { s ->
                    s.copy(
                        devices = s.devices.map { d -> if (d.id == id) d.copy(deviceName = newName) else d },
                        renameSuccess = true,
                    )
                }
            }
        }
    }

    fun clearRenameSuccess() = _state.update { it.copy(renameSuccess = false) }
    fun clearSaveSuccess() = _state.update { it.copy(saveSuccess = false) }
    fun clearError() = _state.update { it.copy(error = null) }
}
