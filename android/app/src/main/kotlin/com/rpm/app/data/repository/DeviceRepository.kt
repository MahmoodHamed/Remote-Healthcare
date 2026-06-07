package com.rpm.app.data.repository

import com.rpm.app.BuildConfig
import com.rpm.app.data.auth.SessionManager
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.remote.dto.PairingInfoDto
import com.rpm.app.data.remote.dto.RenameDeviceRequest
import com.rpm.app.data.remote.dto.SavePairingInfoRequest
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class PairingInfoResult(
    val info: PairingInfoDto,
    val savedLocally: Boolean = false,
)

@Singleton
class DeviceRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
    private val tokenStore: TokenDataStore,
) {
    suspend fun getMyDevices(): Resource<List<DeviceDto>> {
        val result = sessionManager.safeCall { api.getMyDevices() }
        return when (result) {
            is Resource.Success -> result
            is Resource.Error -> if (result.httpCode == 404) Resource.Success(emptyList()) else result
            Resource.Loading -> result
        }
    }

    suspend fun getDevicePairingInfo(): Resource<PairingInfoResult> {
        when (val result = sessionManager.safeCall { api.getDevicePairingInfo() }) {
            is Resource.Success -> {
                val code = result.data.patientId.trim()
                if (code.isNotEmpty()) tokenStore.saveWatchShortCode(code)
                return Resource.Success(PairingInfoResult(result.data, savedLocally = false))
            }
            is Resource.Error -> return when (result.httpCode) {
                404, 502, 503 -> buildLocalPairingInfo()
                else -> Resource.Error(result.message, result.httpCode)
            }
            Resource.Loading -> return Resource.Loading
        }
    }

    suspend fun saveDevicePairingInfo(patientId: String): Resource<PairingInfoResult> {
        val code = patientId.trim().uppercase()
        when (val result = sessionManager.safeCall { api.saveDevicePairingInfo(SavePairingInfoRequest(code)) }) {
            is Resource.Success -> {
                tokenStore.saveWatchShortCode(code)
                return Resource.Success(PairingInfoResult(result.data, savedLocally = false))
            }
            is Resource.Error -> return when (result.httpCode) {
                404, 502, 503 -> saveLocalPairingInfo(code)
                else -> Resource.Error(result.message, result.httpCode)
            }
            Resource.Loading -> return Resource.Loading
        }
    }

    suspend fun renameDevice(id: String, newName: String): Resource<Unit> =
        sessionManager.safeCall { api.renameDevice(id, RenameDeviceRequest(newName)) }

    suspend fun getPatientDevices(patientId: String): Resource<List<DeviceDto>> {
        val result = sessionManager.safeCall { api.getPatientDevices(patientId) }
        return when (result) {
            is Resource.Success -> result
            is Resource.Error -> if (result.httpCode == 404) Resource.Success(emptyList()) else result
            Resource.Loading -> result
        }
    }

    private suspend fun buildLocalPairingInfo(): Resource<PairingInfoResult> {
        val userId = tokenStore.userId.firstOrNull()?.trim().orEmpty()
        if (userId.isBlank()) {
            return Resource.Error("Sign in again to load watch pairing info.")
        }
        val code = tokenStore.getWatchShortCode()?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_PATIENT_ID
        return Resource.Success(
            PairingInfoResult(
                info = PairingInfoDto(
                    patientId = code,
                    streamingPatientId = userId,
                    mqttHost = BuildConfig.MQTT_HOST,
                    mqttPort = BuildConfig.MQTT_PORT,
                ),
                savedLocally = true,
            ),
        )
    }

    private suspend fun saveLocalPairingInfo(code: String): Resource<PairingInfoResult> {
        val userId = tokenStore.userId.firstOrNull()?.trim().orEmpty()
        if (userId.isBlank()) {
            return Resource.Error("Sign in again to save watch pairing info.")
        }
        tokenStore.saveWatchShortCode(code)
        return Resource.Success(
            PairingInfoResult(
                info = PairingInfoDto(
                    patientId = code,
                    streamingPatientId = userId,
                    mqttHost = BuildConfig.MQTT_HOST,
                    mqttPort = BuildConfig.MQTT_PORT,
                ),
                savedLocally = true,
            ),
        )
    }
}

private suspend fun <T> SessionManager.safeCall(
    call: suspend () -> retrofit2.Response<T>,
): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) Resource.Success(body)
            else Resource.Error("Empty response from server", response.code())
        } else {
            Resource.Error(httpErrorMessage(response), response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Network error")
    }
}
