package com.rpm.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Auth ──────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val fcmToken: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String,              // "Doctor" | "Patient" | "Relative"
    val fcmToken: String? = null
)

@Serializable
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String
)

@Serializable
data class LoginResponseDto(
    val tokens: AuthTokensDto,
    val user: UserProfileDto
)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val avatarUrl: String? = null
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class UpdateFcmTokenRequest(val fcmToken: String)


// ── Vitals ────────────────────────────────────────────────────────────────

@Serializable
data class VitalRecordDto(
    val id: String,
    val patientId: String,
    val recordedAt: String,
    val heartRateBpm: Float? = null,
    val spO2Percent: Float? = null,
    val systolicBp: Float? = null,
    val diastolicBp: Float? = null,
    val temperatureC: Float? = null,
    val stepsCount: Int? = null,
    val caloriesBurned: Float? = null,
    val fallDetected: Boolean = false,
    val isWearing: Boolean = true
)

@Serializable
data class VitalsPagedDto(
    val items: List<VitalRecordDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class AlertThresholdDto(
    val maxHeartRateBpm: Float? = null,
    val minHeartRateBpm: Float? = null,
    val minSpO2Percent: Float? = null,
    val maxTemperatureC: Float? = null,
    val maxSystolicBp: Float? = null
)


// ── Alerts ────────────────────────────────────────────────────────────────

@Serializable
data class AlertDto(
    val id: String,
    val patientId: String,
    val patientName: String,
    val type: String,
    val severity: String,
    val status: String,
    val message: String,
    val triggeredAt: String,
    val resolvedAt: String? = null,
    val resolvedByName: String? = null
)

@Serializable
data class AlertPagedDto(
    val items: List<AlertDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)


// ── Chat ──────────────────────────────────────────────────────────────────

@Serializable
data class ConversationDto(
    val id: String,
    val title: String? = null,
    val type: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val participants: List<ParticipantDto> = emptyList()
)

@Serializable
data class ParticipantDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String? = null
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String,
    val sentAt: String,
    val fileUrl: String? = null
)

@Serializable
data class MessagePagedDto(
    val items: List<MessageDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class CreateConversationRequest(
    val participantUserIds: List<String>,
    val title: String? = null
)

@Serializable
data class SendMessageRequest(
    val content: String,
    val type: String = "Text"
)


// ── Patients ──────────────────────────────────────────────────────────────

@Serializable
data class PatientSummaryDto(
    val patientId: String,
    val userId: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val latestVitals: VitalRecordDto? = null,
    val unresolvedAlertCount: Int = 0
)

@Serializable
data class PatientDetailDto(
    val patientId: String,
    val userId: String,
    val fullName: String,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val medicalHistory: String? = null,
    val avatarUrl: String? = null,
    val latestVitals: VitalRecordDto? = null,
    val doctor: DoctorDto? = null
)

@Serializable
data class DoctorDto(
    val userId: String,
    val fullName: String,
    val specialization: String? = null
)

@Serializable
data class AssignDoctorRequest(val doctorUserId: String)

@Serializable
data class LinkRelativeRequest(
    val relativeUserId: String,
    val relationship: String
)
