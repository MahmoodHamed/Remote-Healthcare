package com.rpm.app.ui.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.BuildConfig
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.remote.dto.PairingInfoDto
import com.rpm.app.data.repository.DeviceRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceManagementUiState(
    val devices: List<DeviceDto> = emptyList(),
    val pairingInfo: PairingInfoDto? = null,
    val shortCodeInput: String = BuildConfig.DEFAULT_PATIENT_ID,
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
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceManagementUiState())
    val state: StateFlow<DeviceManagementUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            var err: String? = null

            val devices = when (val devResult = repo.getMyDevices()) {
                is Resource.Success -> devResult.data
                is Resource.Error -> {
                    if (devResult.httpCode != 404) err = devResult.message
                    emptyList()
                }
                else -> emptyList()
            }

            var pairingInfo: PairingInfoDto? = null
            var shortCode = ""
            var localOnly = false
            when (val pairResult = repo.getDevicePairingInfo()) {
                is Resource.Success -> {
                    pairingInfo = pairResult.data.info
                    shortCode = pairResult.data.info.patientId
                    localOnly = pairResult.data.savedLocally
                }
                is Resource.Error -> err = pairResult.message
                else -> {}
            }

            _state.update {
                it.copy(
                    devices = devices,
                    pairingInfo = pairingInfo,
                    shortCodeInput = shortCode.ifBlank { BuildConfig.DEFAULT_PATIENT_ID },
                    savedLocally = localOnly,
                    isLoading = false,
                    error = if (pairingInfo == null && err != null) err else null,
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
                    _state.update {
                        it.copy(
                            pairingInfo = result.data.info,
                            shortCodeInput = result.data.info.patientId,
                            savedLocally = result.data.savedLocally,
                            isSaving = false,
                            saveSuccess = true,
                        )
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
