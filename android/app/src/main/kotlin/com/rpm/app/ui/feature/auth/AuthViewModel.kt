package com.rpm.app.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.repository.AuthRepository
import com.rpm.app.domain.model.Resource
import com.rpm.app.fcm.FcmTokenRegistrar
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
    val userId: String? = null,
    val userRole: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenDataStore,
    private val fcmRegistrar: FcmTokenRegistrar,
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
            val userId = tokenStore.userId.firstOrNull()
            if (token != null) {
                _uiState.value = AuthUiState(isLoggedIn = true, userRole = role, userId = userId)
                launch { fcmRegistrar.registerCurrentToken() }
            }
        }
    }

    fun login(email: String, password: String) = runAuth { authRepository.login(email, password, null) }

    fun adminLogin(email: String, password: String) =
        runAuth { authRepository.adminLogin(email, password, null) }

    fun registerPatient(email: String, password: String, fullName: String) =
        runAuth { authRepository.registerPatient(email, password, fullName, null) }

    fun registerDoctor(email: String, password: String, fullName: String) =
        runAuth { authRepository.registerDoctor(email, password, fullName, null) }

    fun register(email: String, password: String, fullName: String, role: String) =
        runAuth { authRepository.register(email, password, fullName, role, null) }

    private fun runAuth(block: suspend () -> Resource<com.rpm.app.data.remote.dto.LoginResponseDto>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = block()) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        userRole = result.data.user.role,
                        userId = result.data.user.id,
                    )
                    fcmRegistrar.registerCurrentToken()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message,
                )
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
}
