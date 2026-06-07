package com.rpm.app.data.repository

import com.rpm.app.data.auth.SessionManager
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.DeviceDto
import com.rpm.app.data.remote.dto.PairingInfoDto
import com.rpm.app.data.remote.dto.RenameDeviceRequest
import com.rpm.app.data.remote.dto.SavePairingInfoRequest
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
) {
    suspend fun getMyDevices(): Resource<List<DeviceDto>> {
        val result = sessionManager.safeCall { api.getMyDevices() }
        return when (result) {
            is Resource.Success -> result
            is Resource.Error -> if (result.httpCode == 404) Resource.Success(emptyList()) else result
            Resource.Loading -> result
        }
    }

    suspend fun getDevicePairingInfo(): Resource<PairingInfoDto> =
        sessionManager.safeCall { api.getDevicePairingInfo() }

    suspend fun saveDevicePairingInfo(patientId: String): Resource<PairingInfoDto> =
        sessionManager.safeCall { api.saveDevicePairingInfo(SavePairingInfoRequest(patientId)) }

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
