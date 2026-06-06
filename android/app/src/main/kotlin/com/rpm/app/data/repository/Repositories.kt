package com.rpm.app.data.repository

import com.rpm.app.data.auth.SessionManager
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.*
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import javax.inject.Inject

class PatientRepository @Inject constructor(
    private val api: RpmApiService,
    private val tokenStore: TokenDataStore,
    private val sessionManager: SessionManager,
) {

    suspend fun getMyPatients(): Resource<List<PatientSummaryDto>> =
        sessionManager.safeCall { api.getMyPatients() }

    suspend fun getAccessiblePatients(): Resource<List<PatientSummaryDto>> {
        return try {
            val response = api.getAccessiblePatients()
            when {
                response.isSuccessful -> Resource.Success(response.body()!!)
                response.code() == 404 -> loadAccessibleFallback()
                else -> sessionManager.errorFromResponse(response)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private suspend fun loadAccessibleFallback(): Resource<List<PatientSummaryDto>> {
        val role = tokenStore.userRole.firstOrNull()
            ?: return Resource.Error("Please sign in again.")
        val userId = tokenStore.userId.firstOrNull()
            ?: return Resource.Error("Please sign in again.")
        return when (role) {
            "Doctor" -> sessionManager.safeCall { api.getMyPatients() }
            "Patient" -> when (val detail = sessionManager.safeCall { api.getPatientDetail(userId) }) {
                is Resource.Success -> Resource.Success(listOf(detail.data.toSummary()))
                is Resource.Error -> detail
                Resource.Loading -> Resource.Error("Loading…")
            }
            "Relative" -> Resource.Error(
                "Could not load linked family members. Ensure the server supports /api/patients/accessible."
            )
            else -> Resource.Error("Unsupported account type: $role")
        }
    }

    suspend fun getPatientDetail(patientId: String): Resource<PatientDetailDto> =
        sessionManager.safeCall { api.getPatientDetail(patientId) }

    suspend fun getLatestVitals(patientId: String): Resource<VitalRecordDto> =
        sessionManager.safeCall { api.getLatestVitals(patientId) }

    suspend fun getVitals(patientId: String, page: Int = 1): Resource<VitalsPagedDto> {
        val now = Instant.now().toString()
        val weekAgo = Instant.now().minusSeconds(7 * 24 * 3600).toString()
        return sessionManager.safeCall { api.getVitals(patientId, from = weekAgo, to = now, page = page) }
    }

    suspend fun getThresholds(patientId: String): Resource<AlertThresholdDto> =
        sessionManager.safeCall { api.getThresholds(patientId) }

    suspend fun updateThresholds(patientId: String, thresholds: AlertThresholdDto): Resource<Unit> =
        sessionManager.safeCall { api.updateThresholds(patientId, thresholds) }
}

class AlertRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
) {

    suspend fun getAlerts(patientId: String, page: Int = 1): Resource<AlertPagedDto> =
        sessionManager.safeCall { api.getAlerts(patientId, page) }

    suspend fun getUnresolvedAlerts(patientId: String, page: Int = 1): Resource<AlertPagedDto> =
        sessionManager.safeCall { api.getUnresolvedAlerts(patientId, page) }

    suspend fun resolveAlert(patientId: String, alertId: String): Resource<Unit> =
        sessionManager.safeCall { api.resolveAlert(patientId, alertId) }

    suspend fun dismissAlert(patientId: String, alertId: String): Resource<Unit> =
        sessionManager.safeCall { api.dismissAlert(patientId, alertId) }
}

class ChatRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
) {

    suspend fun getConversations(): Resource<List<ConversationDto>> =
        sessionManager.safeCall { api.getConversations() }

    suspend fun createConversation(
        type: String,
        participantIds: List<String>,
        name: String? = null,
    ): Resource<ConversationDto> =
        sessionManager.safeCall { api.createConversation(CreateConversationRequest(type, name, participantIds)) }

    suspend fun findOrCreateDoctorPatientConversation(
        doctorId: String,
        patientId: String,
        patientName: String,
    ): Resource<ConversationDto> {
        val existing = getConversations()
        if (existing is Resource.Success) {
            existing.data.firstOrNull { conv ->
                conv.type == "DoctorPatient" &&
                    conv.participants.any { it.userId.equals(doctorId, ignoreCase = true) } &&
                    conv.participants.any { it.userId.equals(patientId, ignoreCase = true) }
            }?.let { return Resource.Success(it) }
        }
        return createConversation(
            type = "DoctorPatient",
            name = "Care: $patientName",
            participantIds = listOf(doctorId, patientId),
        )
    }

    suspend fun getMessages(conversationId: String, page: Int = 1): Resource<MessagePagedDto> =
        sessionManager.safeCall { api.getMessages(conversationId, page) }

    suspend fun sendMessage(conversationId: String, content: String): Resource<MessageDto> =
        sessionManager.safeCall { api.sendMessage(conversationId, SendMessageRequest(content)) }

    suspend fun deleteMessage(messageId: String): Resource<Unit> =
        sessionManager.safeCall { api.deleteMessage(messageId) }
}

class NotificationRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
) {

    suspend fun getNotifications(page: Int = 1): Resource<NotificationPagedDto> =
        sessionManager.safeCall { api.getNotifications(page) }

    suspend fun getUnreadCount(): Resource<Long> {
        return try {
            val response = api.getUnreadNotificationCount()
            if (response.isSuccessful) Resource.Success(response.body()?.count ?: 0L)
            else sessionManager.errorFromResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markRead(id: String): Resource<Unit> =
        sessionManager.safeCall { api.markNotificationRead(id) }

    suspend fun markAllRead(): Resource<Unit> =
        sessionManager.safeCall { api.markAllNotificationsRead() }
}

class DeviceRepository @Inject constructor(
    private val api: RpmApiService,
    private val sessionManager: SessionManager,
) {
    suspend fun getMyDevices(): Resource<List<DeviceDto>> =
        sessionManager.safeCall { api.getMyDevices() }

    suspend fun getDevicePairingInfo(): Resource<PairingInfoDto> =
        sessionManager.safeCall { api.getDevicePairingInfo() }

    suspend fun renameDevice(id: String, newName: String): Resource<Unit> =
        sessionManager.safeCall { api.renameDevice(id, RenameDeviceRequest(newName)) }

    suspend fun getPatientDevices(patientId: String): Resource<List<DeviceDto>> =
        sessionManager.safeCall { api.getPatientDevices(patientId) }
}

private fun PatientDetailDto.toSummary() = PatientSummaryDto(
    userId = userId,
    fullName = fullName,
    email = email,
    avatarUrl = avatarUrl,
    dateOfBirth = dateOfBirth,
    bloodType = bloodType,
    latestVitals = latestVitals,
)

private suspend fun <T> SessionManager.safeCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Resource.Success(response.body()!!)
        } else {
            errorFromResponse(response)
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Network error")
    }
}

private fun SessionManager.errorFromResponse(response: retrofit2.Response<*>): Resource.Error {
    if (response.code() == 401) notifySessionExpired()
    return Resource.Error(httpErrorMessage(response), response.code())
}
