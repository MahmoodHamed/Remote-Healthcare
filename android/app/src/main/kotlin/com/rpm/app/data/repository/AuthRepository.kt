package com.rpm.app.data.repository

import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.*
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: RpmApiService,
    private val tokenStore: TokenDataStore
) {
    suspend fun login(email: String, password: String, fcmToken: String?): Resource<LoginResponseDto> {
        return try {
            val response = api.login(LoginRequest(email, password, fcmToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenStore.saveSession(
                    body.tokens.accessToken,
                    body.tokens.refreshToken,
                    body.user.id,
                    body.user.role,
                    body.user.fullName
                )
                Resource.Success(body)
            } else {
                Resource.Error(httpErrorMessage(response))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: String,
        licenseNumber: String? = null,
        specialization: String? = null,
    ): Resource<LoginResponseDto> {
        return try {
            val response = api.register(
                RegisterRequest(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    password = password,
                    role = role,
                    licenseNumber = licenseNumber,
                    specialization = specialization,
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenStore.saveSession(
                    body.tokens.accessToken,
                    body.tokens.refreshToken,
                    body.user.id,
                    body.user.role,
                    body.user.fullName
                )
                Resource.Success(body)
            } else {
                Resource.Error(httpErrorMessage(response))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun refreshProfile(): Resource<UserProfileDto> {
        return try {
            val response = api.getMe()
            if (response.isSuccessful) {
                val body = response.body()!!
                val existingToken = tokenStore.getAccessToken()
                val refresh = tokenStore.refreshToken.firstOrNull()
                if (existingToken != null && refresh != null) {
                    tokenStore.saveSession(
                        existingToken,
                        refresh,
                        body.id,
                        body.role,
                        body.fullName,
                    )
                }
                Resource.Success(body)
            } else if (response.code() == 404) {
                val role = tokenStore.userRole.firstOrNull()
                val userId = tokenStore.userId.firstOrNull()
                val name = tokenStore.userName.firstOrNull()
                if (role != null && userId != null) {
                    Resource.Success(
                        UserProfileDto(
                            id = userId,
                            email = "",
                            fullName = name.orEmpty(),
                            role = role,
                        )
                    )
                } else {
                    Resource.Error(httpErrorMessage(response))
                }
            } else {
                Resource.Error(httpErrorMessage(response))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenStore.clearSession()
    }
}
