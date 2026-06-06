package com.rpm.app.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.ConversationDto
import com.rpm.app.data.remote.dto.MessageDto
import com.rpm.app.data.repository.ChatRepository
import com.rpm.app.data.signalr.ChatSignalRClient
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState(isLoading = true))
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init { loadConversations() }

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
)

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val chatSignalR: ChatSignalRClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(ChatRoomUiState(isLoading = true))
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        subscribeRealtime()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = when (val result = repo.getMessages(conversationId)) {
                is Resource.Success -> ChatRoomUiState(messages = result.data.items.reversed())
                is Resource.Error -> ChatRoomUiState(error = result.message)
                Resource.Loading -> ChatRoomUiState(isLoading = true)
            }
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            chatSignalR.connect(conversationId)
            chatSignalR.messages.collect { message ->
                if (!message.conversationId.equals(conversationId, ignoreCase = true)) return@collect
                val current = _uiState.value.messages
                if (current.any { it.id == message.id }) return@collect
                _uiState.value = _uiState.value.copy(messages = current + message)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val result = repo.sendMessage(conversationId, content)
            if (result is Resource.Success) {
                val msg = result.data
                val current = _uiState.value.messages
                if (current.none { it.id == msg.id }) {
                    _uiState.value = _uiState.value.copy(
                        messages = current + msg,
                        isSending = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSending = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isSending = false)
            }
        }
    }

    override fun onCleared() {
        chatSignalR.disconnect()
        super.onCleared()
    }
}
