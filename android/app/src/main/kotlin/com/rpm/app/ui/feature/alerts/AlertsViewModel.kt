package com.rpm.app.ui.feature.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.AlertDto
import com.rpm.app.data.repository.AlertRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<AlertDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repo: AlertRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: String? = savedStateHandle["patientId"]

    private val _uiState = MutableStateFlow(AlertsUiState(isLoading = true))
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init { loadAlerts() }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = AlertsUiState(isLoading = true)
            val result = if (patientId != null) repo.getAlerts(patientId)
                         else repo.getUnresolvedAlerts()
            _uiState.value = when (result) {
                is Resource.Success -> AlertsUiState(alerts = result.data.items)
                is Resource.Error   -> AlertsUiState(error = result.message)
                Resource.Loading    -> AlertsUiState(isLoading = true)
            }
        }
    }

    fun resolve(alertId: String) {
        viewModelScope.launch {
            repo.resolveAlert(alertId)
            loadAlerts()
        }
    }

    fun dismiss(alertId: String) {
        viewModelScope.launch {
            repo.dismissAlert(alertId)
            loadAlerts()
        }
    }
}
