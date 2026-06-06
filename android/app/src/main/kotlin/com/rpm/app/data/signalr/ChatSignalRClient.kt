package com.rpm.app.data.signalr

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.MessageDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSignalRClient @Inject constructor(
    private val tokenStore: TokenDataStore,
) {
    private var hub: HubConnection? = null
    private var joinedConversationId: String? = null
    private val baseUrl: String = com.rpm.app.BuildConfig.SIGNALR_URL

    private val _messages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 64)
    val messages: SharedFlow<MessageDto> = _messages.asSharedFlow()

    fun connect(conversationId: String) {
        if (joinedConversationId == conversationId && hub != null) return
        disconnect()
        val token = runBlocking { tokenStore.getAccessToken() } ?: return
        hub = HubConnectionBuilder
            .create("${baseUrl}hubs/chat?access_token=$token")
            .build()

        hub!!.on(
            "ReceiveMessage",
            { message: MessageDto -> _messages.tryEmit(message) },
            MessageDto::class.java,
        )

        hub!!.start().blockingAwait()
        hub!!.send("JoinConversation", conversationId)
        joinedConversationId = conversationId
    }

    fun disconnect() {
        joinedConversationId?.let { id ->
            runCatching { hub?.send("LeaveConversation", id) }
        }
        runCatching { hub?.stop() }
        hub = null
        joinedConversationId = null
    }
}
