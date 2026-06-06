package com.rpm.app.data.repository

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
) {

    suspend fun getMyPatients(): Resource<List<PatientSummaryDto>> = safeCall { api.getMyPatients() }

    suspend fun getAccessiblePatients(): Resource<List<PatientSummaryDto>> {
        return try {
            val response = api.getAccessiblePatients()
            when {
                response.isSuccessful -> Resource.Success(response.body()!!)
                response.code() == 404 -> loadAccessibleFallback()
                else -> Resource.Error(httpErrorMessage(response))
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
            "Doctor" -> safeCall { api.getMyPatients() }
            "Patient" -> when (val detail = safeCall { api.getPatientDetail(userId) }) {
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
        safeCall { api.getPatientDetail(patientId) }

    suspend fun getLatestVitals(patientId: String): Resource<VitalRecordDto> =
        safeCall { api.getLatestVitals(patientId) }

    suspend fun getVitals(patientId: String, page: Int = 1): Resource<VitalsPagedDto> {
        val now = Instant.now().toString()
        val weekAgo = Instant.now().minusSeconds(7 * 24 * 3600).toString()
        return safeCall { api.getVitals(patientId, from = weekAgo, to = now, page = page) }
    }

    suspend fun getThresholds(patientId: String): Resource<AlertThresholdDto> =
        safeCall { api.getThresholds(patientId) }

    suspend fun updateThresholds(patientId: String, thresholds: AlertThresholdDto): Resource<Unit> =
        safeCall { api.updateThresholds(patientId, thresholds) }
}

class AlertRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getAlerts(patientId: String, page: Int = 1): Resource<AlertPagedDto> =
        safeCall { api.getAlerts(patientId, page) }

    suspend fun getUnresolvedAlerts(patientId: String, page: Int = 1): Resource<AlertPagedDto> =
        safeCall { api.getUnresolvedAlerts(patientId, page) }

    suspend fun resolveAlert(patientId: String, alertId: String): Resource<Unit> =
        safeCall { api.resolveAlert(patientId, alertId) }

    suspend fun dismissAlert(patientId: String, alertId: String): Resource<Unit> =
        safeCall { api.dismissAlert(patientId, alertId) }
}

class ChatRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getConversations(): Resource<List<ConversationDto>> =
        safeCall { api.getConversations() }

    suspend fun createConversation(
        type: String,
        participantIds: List<String>,
        name: String? = null,
    ): Resource<ConversationDto> =
        safeCall { api.createConversation(CreateConversationRequest(type, name, participantIds)) }

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
        safeCall { api.getMessages(conversationId, page) }

    suspend fun sendMessage(conversationId: String, content: String): Resource<MessageDto> =
        safeCall { api.sendMessage(conversationId, SendMessageRequest(content)) }

    suspend fun deleteMessage(messageId: String): Resource<Unit> =
        safeCall { api.deleteMessage(messageId) }
}

class NotificationRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getNotifications(page: Int = 1): Resource<List<NotificationDto>> =
        safeCall { api.getNotifications(page) }

    suspend fun getUnreadCount(): Resource<Long> {
        return try {
            val response = api.getUnreadNotificationCount()
            if (response.isSuccessful) Resource.Success(response.body()?.count ?: 0L)
            else Resource.Error(httpErrorMessage(response))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markRead(id: String): Resource<Unit> = safeCall { api.markNotificationRead(id) }

    suspend fun markAllRead(): Resource<Unit> = safeCall { api.markAllNotificationsRead() }
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

// ── Shared helper ─────────────────────────────────────────────────────────
private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error(httpErrorMessage(response))
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Network error")
    }
}
