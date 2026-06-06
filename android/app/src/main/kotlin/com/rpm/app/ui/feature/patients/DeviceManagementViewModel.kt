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

            val devResult = repo.getMyDevices()
            val devices = if (devResult is Resource.Success) devResult.data
            else { err = (devResult as? Resource.Error)?.message; emptyList() }

            val pairResult = repo.getDevicePairingInfo()
            val pairingInfo = if (pairResult is Resource.Success) pairResult.data
            else { if (err == null) err = (pairResult as? Resource.Error)?.message; null }

            _state.update { it.copy(devices = devices, pairingInfo = pairingInfo, isLoading = false, error = err) }
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
