package com.rpm.app.ui.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val usingLocalPairing: Boolean = false,
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
            var localPairing = false
            when (val pairResult = repo.getDevicePairingInfo()) {
                is Resource.Success -> {
                    pairingInfo = pairResult.data.info
                    localPairing = pairResult.data.fromLocalFallback
                }
                is Resource.Error -> err = pairResult.message
                else -> {}
            }

            _state.update {
                it.copy(
                    devices = devices,
                    pairingInfo = pairingInfo,
                    usingLocalPairing = localPairing,
                    isLoading = false,
                    error = if (pairingInfo == null) err else null,
                )
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
    fun clearError() = _state.update { it.copy(error = null) }
}
