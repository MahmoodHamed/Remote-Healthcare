package com.rpm.app.data.remote.api

import com.rpm.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface RpmApiService {

    // ── Auth ──────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponseDto>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthTokensDto>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @PATCH("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: UpdateFcmTokenRequest): Response<Unit>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserProfileDto>

    // ── Patients ──────────────────────────────────────────────────────────
    @GET("api/patients")
    suspend fun getMyPatients(): Response<List<PatientSummaryDto>>

    @GET("api/patients/accessible")
    suspend fun getAccessiblePatients(): Response<List<PatientSummaryDto>>

    @GET("api/patients/{patientId}")
    suspend fun getPatientDetail(@Path("patientId") patientId: String): Response<PatientDetailDto>

    @POST("api/patients/{patientId}/assign-doctor")
    suspend fun assignDoctor(
        @Path("patientId") patientId: String,
        @Body request: AssignDoctorRequest
    ): Response<Unit>

    @POST("api/patients/{patientId}/link-relative")
    suspend fun linkRelative(
        @Path("patientId") patientId: String,
        @Body request: LinkRelativeRequest
    ): Response<Unit>

    // ── Vitals ────────────────────────────────────────────────────────────
    @GET("api/patients/{patientId}/vitals")
    suspend fun getVitals(
        @Path("patientId") patientId: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<VitalsPagedDto>

    @GET("api/patients/{patientId}/vitals/latest")
    suspend fun getLatestVitals(@Path("patientId") patientId: String): Response<VitalRecordDto>

    @GET("api/patients/{patientId}/vitals/threshold")
    suspend fun getThresholds(@Path("patientId") patientId: String): Response<AlertThresholdDto>

    @PUT("api/patients/{patientId}/vitals/threshold")
    suspend fun updateThresholds(
        @Path("patientId") patientId: String,
        @Body thresholds: AlertThresholdDto
    ): Response<Unit>

    // ── Alerts ────────────────────────────────────────────────────────────
    @GET("api/patients/{patientId}/alerts")
    suspend fun getAlerts(
        @Path("patientId") patientId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): Response<AlertPagedDto>

    @GET("api/patients/{patientId}/alerts/unresolved")
    suspend fun getUnresolvedAlerts(
        @Path("patientId") patientId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<AlertPagedDto>

    @POST("api/patients/{patientId}/alerts/{alertId}/resolve")
    suspend fun resolveAlert(
        @Path("patientId") patientId: String,
        @Path("alertId") alertId: String,
    ): Response<Unit>

    @POST("api/patients/{patientId}/alerts/{alertId}/dismiss")
    suspend fun dismissAlert(
        @Path("patientId") patientId: String,
        @Path("alertId") alertId: String,
    ): Response<Unit>

    // ── Chat ──────────────────────────────────────────────────────────────
    @GET("api/chat/conversations")
    suspend fun getConversations(): Response<List<ConversationDto>>

    @POST("api/chat/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<ConversationDto>

    @GET("api/chat/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): Response<MessagePagedDto>

    @POST("api/chat/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<MessageDto>

    @DELETE("api/chat/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<Unit>

    // ── Notifications (push inbox) ────────────────────────────────────────
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
    ): Response<NotificationPagedDto>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<UnreadCountDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // ── Devices / Watch ───────────────────────────────────────────────────
    @GET("api/devices")
    suspend fun getMyDevices(): Response<List<DeviceDto>>

    @GET("api/devices/pairing-info")
    suspend fun getDevicePairingInfo(): Response<PairingInfoDto>

    @PUT("api/devices/pairing-info")
    suspend fun saveDevicePairingInfo(@Body request: SavePairingInfoRequest): Response<PairingInfoDto>

    @PATCH("api/devices/{id}/name")
    suspend fun renameDevice(
        @Path("id") id: String,
        @Body request: RenameDeviceRequest,
    ): Response<Unit>

    @GET("api/patients/{patientId}/devices")
    suspend fun getPatientDevices(@Path("patientId") patientId: String): Response<List<DeviceDto>>
}
