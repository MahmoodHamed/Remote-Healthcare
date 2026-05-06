package com.rpm.app.data.repository

import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.*
import com.rpm.app.domain.model.Resource
import javax.inject.Inject

class PatientRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getMyPatients(): Resource<List<PatientSummaryDto>> = safeCall { api.getMyPatients() }

    suspend fun getPatientDetail(patientId: String): Resource<PatientDetailDto> =
        safeCall { api.getPatientDetail(patientId) }

    suspend fun getLatestVitals(patientId: String): Resource<VitalRecordDto> =
        safeCall { api.getLatestVitals(patientId) }

    suspend fun getVitals(patientId: String, page: Int = 1): Resource<VitalsPagedDto> =
        safeCall { api.getVitals(patientId, page) }

    suspend fun getThresholds(patientId: String): Resource<AlertThresholdDto> =
        safeCall { api.getThresholds(patientId) }

    suspend fun updateThresholds(patientId: String, thresholds: AlertThresholdDto): Resource<Unit> =
        safeCall { api.updateThresholds(patientId, thresholds) }
}

class AlertRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getAlerts(patientId: String, page: Int = 1): Resource<AlertPagedDto> =
        safeCall { api.getAlerts(patientId, page) }

    suspend fun getUnresolvedAlerts(): Resource<AlertPagedDto> =
        safeCall { api.getUnresolvedAlerts() }

    suspend fun resolveAlert(alertId: String): Resource<Unit> =
        safeCall { api.resolveAlert(alertId) }

    suspend fun dismissAlert(alertId: String): Resource<Unit> =
        safeCall { api.dismissAlert(alertId) }
}

class ChatRepository @Inject constructor(private val api: RpmApiService) {

    suspend fun getConversations(): Resource<List<ConversationDto>> =
        safeCall { api.getConversations() }

    suspend fun createConversation(
        participantIds: List<String>,
        title: String? = null
    ): Resource<ConversationDto> =
        safeCall { api.createConversation(CreateConversationRequest(participantIds, title)) }

    suspend fun getMessages(conversationId: String, page: Int = 1): Resource<MessagePagedDto> =
        safeCall { api.getMessages(conversationId, page) }

    suspend fun sendMessage(conversationId: String, content: String): Resource<MessageDto> =
        safeCall { api.sendMessage(conversationId, SendMessageRequest(content)) }

    suspend fun deleteMessage(messageId: String): Resource<Unit> =
        safeCall { api.deleteMessage(messageId) }
}

// ── Shared helper ─────────────────────────────────────────────────────────
private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Network error")
    }
}
