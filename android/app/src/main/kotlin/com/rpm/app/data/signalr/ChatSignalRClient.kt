package com.rpm.app.data.signalr

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

private const val TAG = "ChatSignalR"

@Singleton
class ChatSignalRClient @Inject constructor(
    private val tokenStore: TokenDataStore,
) {
    private var hub: HubConnection? = null
    private var joinedConversationId: String? = null
    private val baseUrl: String = com.rpm.app.BuildConfig.SIGNALR_URL

    private val _messages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 64)
    val messages: SharedFlow<MessageDto> = _messages.asSharedFlow()

    fun publishLocal(message: MessageDto) {
        _messages.tryEmit(message)
    }

    /** Connect on a background thread; retries until connected or coroutine cancelled. */
    suspend fun connect(conversationId: String): Boolean = withContext(Dispatchers.IO) {
        while (coroutineContext.isActive) {
            try {
                if (joinedConversationId == conversationId && hub != null) return@withContext true
                disconnectInternal()
                val token = tokenStore.getAccessToken()
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "No access token — waiting before chat hub connect")
                    delay(3_000)
                    continue
                }
                val connection = HubConnectionBuilder
                    .create("${baseUrl}hubs/chat?access_token=$token")
                    .build()
                connection.on(
                    "ReceiveMessage",
                    { message: MessageDto -> _messages.tryEmit(message) },
                    MessageDto::class.java,
                )
                connection.start().blockingAwait()
                connection.send("JoinConversation", conversationId)
                hub = connection
                joinedConversationId = conversationId
                Log.i(TAG, "Joined conversation $conversationId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Chat hub connect failed: ${e.message}", e)
                disconnectInternal()
                delay(3_000)
            }
        }
        false
    }

    fun disconnect() {
        joinedConversationId?.let { id ->
            runCatching { hub?.send("LeaveConversation", id) }
        }
        disconnectInternal()
    }

    private fun disconnectInternal() {
        runCatching { hub?.stop() }
        hub = null
        joinedConversationId = null
    }
}
