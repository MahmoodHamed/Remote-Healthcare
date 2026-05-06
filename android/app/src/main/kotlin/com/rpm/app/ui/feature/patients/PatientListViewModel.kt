package com.rpm.app.ui.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.PatientSummaryDto
import com.rpm.app.data.repository.PatientRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientListUiState(
    val isLoading: Boolean = false,
    val patients: List<PatientSummaryDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repo: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientListUiState(isLoading = true))
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    init { loadPatients() }

    fun loadPatients() {
        viewModelScope.launch {
            _uiState.value = PatientListUiState(isLoading = true)
            _uiState.value = when (val result = repo.getMyPatients()) {
                is Resource.Success -> PatientListUiState(patients = result.data)
                is Resource.Error   -> PatientListUiState(error = result.message)
                Resource.Loading    -> PatientListUiState(isLoading = true)
            }
        }
    }
}
