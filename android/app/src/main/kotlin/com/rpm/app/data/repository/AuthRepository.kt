package com.rpm.app.data.repository

import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.*
import com.rpm.app.domain.model.Resource
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: RpmApiService,
    private val tokenStore: TokenDataStore
) {
    suspend fun login(email: String, password: String, fcmToken: String?): Resource<LoginResponseDto> =
        runLogin { api.login(LoginRequest(email, password, fcmToken)) }

    suspend fun adminLogin(email: String, password: String, fcmToken: String?): Resource<LoginResponseDto> =
        runLogin { api.adminLogin(LoginRequest(email, password, fcmToken)) }

    suspend fun registerPatient(
        email: String, password: String, fullName: String, fcmToken: String?
    ): Resource<LoginResponseDto> =
        runLogin { api.registerPatient(RegisterRequest(email, password, fullName, "Patient", fcmToken)) }

    suspend fun registerDoctor(
        email: String, password: String, fullName: String, fcmToken: String?
    ): Resource<LoginResponseDto> =
        runLogin { api.registerDoctor(RegisterRequest(email, password, fullName, "Doctor", fcmToken)) }

    suspend fun register(
        email: String, password: String, fullName: String, role: String, fcmToken: String?
    ): Resource<LoginResponseDto> =
        runLogin { api.register(RegisterRequest(email, password, fullName, role, fcmToken)) }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenStore.clearSession()
    }

    private suspend fun runLogin(
        call: suspend () -> retrofit2.Response<LoginResponseDto>,
    ): Resource<LoginResponseDto> = try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()!!
            tokenStore.saveSession(
                body.tokens.accessToken,
                body.tokens.refreshToken,
                body.user.id,
                body.user.role,
                body.user.fullName,
            )
            Resource.Success(body)
        } else {
            Resource.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unknown error")
    }
}
