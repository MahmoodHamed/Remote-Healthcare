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
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String,              // "Doctor" | "Patient" | "Relative"
    val licenseNumber: String? = null,
    val specialization: String? = null,
)

@Serializable
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String? = null,
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
    val phone: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class RefreshTokenRequest(
    val accessToken: String,
    val refreshToken: String,
    val deviceInfo: String? = null,
)

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
    val skinTemperatureC: Float? = null,
    val ambientTemperatureC: Float? = null,
    val hrvMs: Float? = null,
    val stressScore: Float? = null,
    val bodyFatPercent: Float? = null,
    val ecgAvgHeartRateBpm: Float? = null,
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
    val name: String? = null,
    val type: String,
    val lastMessageAt: String? = null,
    val participants: List<ParticipantDto> = emptyList(),
) {
    /** Display title — maps backend `name` field. */
    val title: String? get() = name
}

@Serializable
data class ParticipantDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val isAdmin: Boolean = false,
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
    val mediaUrl: String? = null,
    val isDeleted: Boolean = false,
)

@Serializable
data class MessagePagedDto(
    val items: List<MessageDto>,
    val totalCount: Long = 0,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class CreateConversationRequest(
    val type: String,
    val name: String? = null,
    val participantIds: List<String>,
)

@Serializable
data class SendMessageRequest(
    val content: String,
    val type: String = "Text"
)


// ── Patients ──────────────────────────────────────────────────────────────

@Serializable
data class VitalRecordLatestDto(
    val heartRateBpm: Float? = null,
    val spO2Percent: Float? = null,
    val systolicBp: Float? = null,
    val diastolicBp: Float? = null,
    val temperatureC: Float? = null,
    val skinTemperatureC: Float? = null,
    val hrvMs: Float? = null,
    val stressScore: Float? = null,
    val bodyFatPercent: Float? = null,
    val ecgAvgHeartRateBpm: Float? = null,
    val isWearing: Boolean = true,
    val recordedAt: String? = null,
)

@Serializable
data class PatientSummaryDto(
    val userId: String,
    val fullName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val latestVitals: VitalRecordLatestDto? = null,
)

@Serializable
data class PatientDetailDto(
    val userId: String,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val weightKg: Float? = null,
    val heightCm: Float? = null,
    val chronicDiseases: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val emergencyContactPhone: String? = null,
    val latestVitals: VitalRecordLatestDto? = null,
    val doctor: DoctorDto? = null,
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


// ── Push notification inbox ───────────────────────────────────────────────

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val alertId: String? = null,
    val isRead: Boolean = false,
    val sentAt: String,
)

@Serializable
data class NotificationPagedDto(
    val items: List<NotificationDto>,
    val unreadCount: Long = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class UnreadCountDto(val count: Long = 0)


// ── Devices / Watch pairing ───────────────────────────────────────────────

@Serializable
data class DeviceDto(
    val id: String,
    val deviceName: String,
    val deviceModel: String,
    val status: String,                // "Online" | "Offline" | "LowBattery"
    val batteryLevel: Float? = null,
    val lastSeenAt: String? = null,
    val registeredAt: String,
)

@Serializable
data class PairingInfoDto(
    val patientId: String,
    val mqttHost: String,
    val mqttPort: Int,
)

@Serializable
data class RenameDeviceRequest(val newName: String)
