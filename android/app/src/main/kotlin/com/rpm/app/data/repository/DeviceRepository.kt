package com.rpm.app.data.repository

import com.rpm.app.BuildConfig
import com.rpm.app.data.auth.SessionManager
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.signalr.PatientShortCode
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.remote.dto.PairingInfoDto
import com.rpm.app.data.remote.dto.RenameDeviceRequest
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class PairingInfoResult(
    val info: PairingInfoDto,
    val fromLocalFallback: Boolean = false,
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
            is Resource.Success -> return Resource.Success(
                PairingInfoResult(result.data, fromLocalFallback = false),
            )
            is Resource.Error -> return when (result.httpCode) {
                404, 502, 503 -> buildLocalPairingInfo()
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

    /** Used when /api/devices/pairing-info is not deployed yet on the server. */
    private suspend fun buildLocalPairingInfo(): Resource<PairingInfoResult> {
        val userId = tokenStore.userId.firstOrNull()?.trim().orEmpty()
        if (userId.isBlank()) {
            return Resource.Error("Sign in again to load watch pairing info.")
        }
        return Resource.Success(
            PairingInfoResult(
                info = PairingInfoDto(
                    patientId = PatientShortCode.fromUserId(userId),
                    streamingPatientId = userId,
                    mqttHost = BuildConfig.MQTT_HOST,
                    mqttPort = BuildConfig.MQTT_PORT,
                ),
                fromLocalFallback = true,
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
