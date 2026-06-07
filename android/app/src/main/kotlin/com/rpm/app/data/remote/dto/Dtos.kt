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
    val deviceId: String? = null,
    val recordedAt: String,

    // Cardio
    val heartRateBpm: Float? = null,
    val heartRateVariabilityMs: Float? = null,
    val restingHeartRateBpm: Float? = null,
    val maxHeartRateBpm: Float? = null,

    // Respiratory
    val spO2Percent: Float? = null,
    val respirationRateBpm: Float? = null,

    // Blood pressure
    val systolicBp: Float? = null,
    val diastolicBp: Float? = null,

    // Temperature
    val temperatureC: Float? = null,
    val skinTemperatureC: Float? = null,

    // Activity & energy
    val stepsCount: Int? = null,
    val caloriesBurned: Float? = null,
    val distanceMeters: Float? = null,
    val floorsClimbed: Int? = null,
    val activeMinutes: Int? = null,

    // Sleep & stress
    val stressScore: Float? = null,
    val sleepScore: Float? = null,
    val sleepDurationMinutes: Int? = null,

    // Body composition
    val bodyFatPercent: Float? = null,
    val muscleMassKg: Float? = null,
    val bodyWaterPercent: Float? = null,
    val basalMetabolicRate: Float? = null,

    // ECG
    val ecgAverageHeartRate: Float? = null,
    val ecgClassification: String? = null,
    val ecgWaveformJson: String? = null,

    // Glucose
    val bloodGlucoseMgDl: Float? = null,

    // Safety & wear status
    val batteryLevel: Float? = null,
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
    val maxSkinTemperatureC: Float? = null,
    val maxSystolicBp: Float? = null,
    val minRespirationRate: Float? = null,
    val maxRespirationRate: Float? = null,
    val maxStressScore: Float? = null,
    val minBloodGlucoseMgDl: Float? = null,
    val maxBloodGlucoseMgDl: Float? = null
)


// ── Notifications ─────────────────────────────────────────────────────────

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val sentAt: String,
    val alertId: String? = null,
    val dataPayload: String? = null
)

@Serializable
data class NotificationsPagedDto(
    val items: List<NotificationDto>,
    val unreadCount: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UnreadCountDto(val count: Int)


// ── Alerts ────────────────────────────────────────────────────────────────

@Serializable
data class AlertDto(
    val id: String,
    val patientId: String,
    val type: String,
    val severity: String,
    val status: String,
    val message: String,
    val triggeredAt: String,
    val resolvedAt: String? = null,
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


@Serializable
data class ResolveAlertRequest(val notes: String? = null)

// ── Patients ──────────────────────────────────────────────────────────────

@Serializable
data class PatientSummaryDto(
    val userId: String,
    val profileId: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val isActive: Boolean = true,
)

@Serializable
data class VitalRecordLatestDto(
    val heartRateBpm: Float? = null,
    val spO2Percent: Float? = null,
    val systolicBp: Float? = null,
    val diastolicBp: Float? = null,
    val temperatureC: Float? = null,
    val recordedAt: String,
)

@Serializable
data class DoctorAssignmentDto(
    val doctorUserId: String,
    val doctorName: String,
    val specialization: String,
    val status: String,
    val assignedAt: String,
)

@Serializable
data class PatientDetailDto(
    val userId: String,
    val profileId: String,
    val fullName: String,
    val email: String,
    val phone: String,
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
    val doctors: List<DoctorAssignmentDto> = emptyList(),
    /** 6-char short ID for the paired watch; used to derive the streaming patient UUID. */
    val watchShortId: String? = null,
)

@Serializable
data class AssignDoctorRequest(val doctorUserId: String)

@Serializable
data class LinkRelativeRequest(
    val relativeUserId: String,
    val relationship: String
)

@Serializable
data class SetWatchShortIdRequest(val shortId: String?)

@Serializable
data class WatchShortIdResponse(val watchShortId: String?)
