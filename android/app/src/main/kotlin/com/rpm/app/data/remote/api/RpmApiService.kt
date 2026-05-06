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

    @PUT("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: UpdateFcmTokenRequest): Response<Unit>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserProfileDto>

    // ── Patients ──────────────────────────────────────────────────────────
    @GET("api/patients")
    suspend fun getMyPatients(): Response<List<PatientSummaryDto>>

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
    @GET("api/vitals/{patientId}")
    suspend fun getVitals(
        @Path("patientId") patientId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<VitalsPagedDto>

    @GET("api/vitals/{patientId}/latest")
    suspend fun getLatestVitals(@Path("patientId") patientId: String): Response<VitalRecordDto>

    @GET("api/vitals/{patientId}/thresholds")
    suspend fun getThresholds(@Path("patientId") patientId: String): Response<AlertThresholdDto>

    @PUT("api/vitals/{patientId}/thresholds")
    suspend fun updateThresholds(
        @Path("patientId") patientId: String,
        @Body thresholds: AlertThresholdDto
    ): Response<Unit>

    // ── Alerts ────────────────────────────────────────────────────────────
    @GET("api/alerts/{patientId}")
    suspend fun getAlerts(
        @Path("patientId") patientId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): Response<AlertPagedDto>

    @GET("api/alerts/unresolved")
    suspend fun getUnresolvedAlerts(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<AlertPagedDto>

    @POST("api/alerts/{alertId}/resolve")
    suspend fun resolveAlert(@Path("alertId") alertId: String): Response<Unit>

    @POST("api/alerts/{alertId}/dismiss")
    suspend fun dismissAlert(@Path("alertId") alertId: String): Response<Unit>

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
}
