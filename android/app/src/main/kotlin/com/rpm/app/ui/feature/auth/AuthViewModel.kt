package com.rpm.app.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.auth.SessionManager
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
    /** False until cached session is read / validated on cold start. */
    val isSessionReady: Boolean = false,
    /** True when the session expired automatically (not a manual logout). */
    val showSessionExpiredDialog: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenDataStore,
    private val fcmTokenRegistrar: FcmTokenRegistrar,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
        viewModelScope.launch {
            sessionManager.sessionExpired.collect {
                navigateToLoginDueToExpiry()
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            refreshSession()
        }
    }

    private suspend fun refreshSession() {
        val token = tokenStore.getAccessToken()
        if (token == null) {
            _uiState.value = AuthUiState(isSessionReady = true)
            return
        }

        val cachedRole = tokenStore.userRole.firstOrNull()
        val cachedUserId = tokenStore.userId.firstOrNull()
        if (cachedRole != null && cachedUserId != null) {
            sessionManager.reset()
            _uiState.value = AuthUiState(
                isLoggedIn = true,
                userRole = cachedRole,
                userId = cachedUserId,
                isSessionReady = true,
            )
        }

        when (val result = authRepository.refreshProfile()) {
            is Resource.Success -> {
                sessionManager.reset()
                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = result.data.role,
                    userId = result.data.id,
                    isSessionReady = true,
                )
                registerFcmToken()
            }
            is Resource.Error -> {
                if (result.httpCode == 401) {
                    clearSessionAndNavigateToLogin()
                    _uiState.value = AuthUiState(isSessionReady = true)
                    return
                }
                if (cachedRole != null && cachedUserId != null) {
                    sessionManager.reset()
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = cachedRole,
                        userId = cachedUserId,
                        isSessionReady = true,
                    )
                    registerFcmToken()
                } else {
                    clearSessionAndNavigateToLogin()
                    _uiState.value = AuthUiState(isSessionReady = true)
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
                    sessionManager.reset()
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = result.data.user.role,
                        userId = result.data.user.id,
                        isSessionReady = true,
                    )
                    registerFcmToken()
                }
                is Resource.Error -> _uiState.value = AuthUiState(
                    error = result.message,
                    isSessionReady = true,
                )
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
                    sessionManager.reset()
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = result.data.user.role,
                        userId = result.data.user.id,
                        isSessionReady = true,
                    )
                    registerFcmToken()
                }
                is Resource.Error -> _uiState.value = AuthUiState(
                    error = result.message,
                    isSessionReady = true,
                )
                Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            clearSessionAndNavigateToLogin()
            _uiState.value = AuthUiState(isSessionReady = true)
        }
    }

    private suspend fun clearSessionAndNavigateToLogin() {
        authRepository.logout()
        sessionManager.reset()
        _uiState.value = AuthUiState(isSessionReady = true)
    }

    /** Called when the server returns 401 — shows an expiry notice before going to login. */
    private suspend fun navigateToLoginDueToExpiry() {
        authRepository.logout()
        sessionManager.reset()
        _uiState.value = AuthUiState(isSessionReady = true, showSessionExpiredDialog = true)
    }

    fun dismissSessionExpiredDialog() {
        _uiState.value = _uiState.value.copy(showSessionExpiredDialog = false)
    }

    private fun registerFcmToken() {
        viewModelScope.launch {
            fcmTokenRegistrar.registerToken()
        }
    }
}
