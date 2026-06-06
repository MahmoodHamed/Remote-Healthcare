package com.rpm.app.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.ConversationDto
import com.rpm.app.data.remote.dto.MessageDto
import com.rpm.app.data.repository.ChatRepository
import com.rpm.app.data.signalr.ChatSignalRClient
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationListUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val chatSignalR: ChatSignalRClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState(isLoading = true))
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        viewModelScope.launch {
            chatSignalR.messages.collect {
                loadConversations()
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = ConversationListUiState(isLoading = true)
            _uiState.value = when (val result = repo.getConversations()) {
                is Resource.Success -> ConversationListUiState(conversations = result.data)
                is Resource.Error -> ConversationListUiState(error = result.message)
                Resource.Loading -> ConversationListUiState(isLoading = true)
            }
        }
    }
}

data class ChatRoomUiState(
    val isLoading: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
    val error: String? = null,
    val isSending: Boolean = false,
    val conversationTitle: String = "Chat",
)

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val chatSignalR: ChatSignalRClient,
    private val tokenStore: TokenDataStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    val currentUserId: StateFlow<String?> = tokenStore.userId.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private val _uiState = MutableStateFlow(ChatRoomUiState(isLoading = true))
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        loadConversationMeta()
        loadMessages()
        subscribeRealtime()
    }

    private fun loadConversationMeta() {
        viewModelScope.launch {
            when (val result = repo.getConversations()) {
                is Resource.Success -> {
                    val conv = result.data.firstOrNull { it.id.equals(conversationId, ignoreCase = true) }
                    val title = conv?.title
                        ?: conv?.participants?.joinToString { it.fullName }
                        ?: "Chat"
                    _uiState.value = _uiState.value.copy(conversationTitle = title)
                }
                else -> {}
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _uiState.value = when (val result = repo.getMessages(conversationId)) {
                is Resource.Success -> ChatRoomUiState(
                    conversationTitle = _uiState.value.conversationTitle,
                    messages = ChatMessageUtils.sortAscending(result.data.items),
                )
                is Resource.Error -> ChatRoomUiState(
                    conversationTitle = _uiState.value.conversationTitle,
                    error = result.message,
                )
                Resource.Loading -> ChatRoomUiState(isLoading = true)
            }
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            launch { chatSignalR.connect(conversationId) }
            chatSignalR.messages.collect { message ->
                appendMessage(message)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val trimmed = content.trim()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)
            when (val result = repo.sendMessage(conversationId, trimmed)) {
                is Resource.Success -> {
                    appendMessage(result.data)
                    chatSignalR.publishLocal(result.data)
                    _uiState.value = _uiState.value.copy(isSending = false)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.message,
                    )
                }
                Resource.Loading -> {}
            }
        }
    }

    private fun appendMessage(message: MessageDto) {
        if (!message.conversationId.equals(conversationId, ignoreCase = true)) return
        val current = _uiState.value.messages
        if (current.any { it.id.equals(message.id, ignoreCase = true) }) return
        _uiState.value = _uiState.value.copy(
            messages = ChatMessageUtils.sortAscending(current + message),
        )
    }

    override fun onCleared() {
        chatSignalR.disconnect()
        super.onCleared()
    }
}
