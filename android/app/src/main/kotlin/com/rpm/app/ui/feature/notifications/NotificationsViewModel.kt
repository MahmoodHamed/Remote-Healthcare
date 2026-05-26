package com.rpm.app.ui.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.NotificationDto
import com.rpm.app.data.repository.NotificationsRepository
import com.rpm.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val res = repo.getNotifications()) {
                is Resource.Success -> _state.value = NotificationsUiState(
                    items = res.data.items,
                    unreadCount = res.data.unreadCount,
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = res.message,
                )
                Resource.Loading -> {}
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            repo.markRead(id)
            refresh()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repo.markAllRead()
            refresh()
        }
    }
}
