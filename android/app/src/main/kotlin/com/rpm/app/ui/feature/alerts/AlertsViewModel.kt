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

enum class AlertFilter(val label: String) {
    All("All"),
    Active("Active"),
    Resolved("Resolved"),
    Dismissed("Dismissed"),
}

data class AlertsUiState(
    val isLoading: Boolean    = false,
    val alerts: List<AlertDto> = emptyList(),
    val error: String?        = null,
    val filter: AlertFilter   = AlertFilter.Active,
    val actionError: String?  = null,
) {
    val filtered: List<AlertDto>
        get() = if (filter == AlertFilter.All) alerts
                else alerts.filter { it.status.equals(filter.label, ignoreCase = true) }
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repo: AlertRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val patientId: String? = savedStateHandle["patientId"]

    private val _uiState = MutableStateFlow(AlertsUiState(isLoading = true))
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init { loadAlerts() }

    fun loadAlerts() {
        val pid = patientId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, actionError = null)
            val result = if (pid != null) {
                repo.getAlerts(pid)
            } else {
                Resource.Error("Select a patient first to view their alerts.")
            }
            _uiState.value = when (result) {
                is Resource.Success -> _uiState.value.copy(isLoading = false, alerts = result.data.items, error = null)
                is Resource.Error   -> _uiState.value.copy(isLoading = false, error = result.message)
                Resource.Loading    -> _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun setFilter(filter: AlertFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun resolve(alertId: String) {
        val pid = patientId ?: return
        viewModelScope.launch {
            val result = repo.resolveAlert(pid, alertId)
            if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(actionError = result.message)
            }
            loadAlerts()
        }
    }

    fun dismiss(alertId: String) {
        val pid = patientId ?: return
        viewModelScope.launch {
            val result = repo.dismissAlert(pid, alertId)
            if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(actionError = result.message)
            }
            loadAlerts()
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
