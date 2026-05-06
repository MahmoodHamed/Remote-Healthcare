package com.rpm.app.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.repository.AuthRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val token = tokenStore.getAccessToken()
            val role  = tokenStore.userRole.firstOrNull()
            if (token != null) {
                _uiState.value = AuthUiState(isLoggedIn = true, userRole = role)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(email, password, fcmToken = null)) {
                is Resource.Success -> _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = result.data.user.role
                )
                is Resource.Error   -> _uiState.value = AuthUiState(error = result.message)
                Resource.Loading    -> {}
            }
        }
    }

    fun register(email: String, password: String, fullName: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(email, password, fullName, role, null)) {
                is Resource.Success -> _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = result.data.user.role
                )
                is Resource.Error   -> _uiState.value = AuthUiState(error = result.message)
                Resource.Loading    -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }
}
