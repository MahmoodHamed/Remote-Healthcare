package com.rpm.app.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.fcm.FcmTokenRegistrar
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
    val userRole: String? = null,
    val userId: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenDataStore,
    private val fcmTokenRegistrar: FcmTokenRegistrar,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            refreshSession()
        }
    }

    private suspend fun refreshSession() {
        if (tokenStore.getAccessToken() == null) return
        when (val result = authRepository.refreshProfile()) {
            is Resource.Success -> {
                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = result.data.role,
                    userId = result.data.id,
                )
                registerFcmToken()
            }
            is Resource.Error -> {
                val role = tokenStore.userRole.firstOrNull()
                val userId = tokenStore.userId.firstOrNull()
                if (role != null && userId != null) {
                    _uiState.value = AuthUiState(isLoggedIn = true, userRole = role, userId = userId)
                    registerFcmToken()
                } else {
                    authRepository.logout()
                    _uiState.value = AuthUiState()
                }
            }
            Resource.Loading -> {}
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(email, password, fcmToken = null)) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = result.data.user.role,
                        userId = result.data.user.id,
                    )
                    registerFcmToken()
                }
                is Resource.Error -> _uiState.value = AuthUiState(error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: String,
        licenseNumber: String? = null,
        specialization: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(
                email, password, fullName, phone, role, licenseNumber, specialization,
            )) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = result.data.user.role,
                        userId = result.data.user.id,
                    )
                    registerFcmToken()
                }
                is Resource.Error -> _uiState.value = AuthUiState(error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    private fun registerFcmToken() {
        viewModelScope.launch {
            fcmTokenRegistrar.registerToken()
        }
    }
}
