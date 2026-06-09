package com.rpm.app.ui.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpm.app.data.remote.dto.NotificationDto
import com.rpm.app.data.repository.NotificationRepository
import com.rpm.app.domain.model.Resource
import com.rpm.app.ui.feature.patients.formatVitalTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──────────────────────────────────────────────────────────────

data class NotificationsUiState(
    val isLoading: Boolean               = false,
    val notifications: List<NotificationDto> = emptyList(),
    val error: String?                   = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState(isLoading = true)
            when (val result = repo.getNotifications()) {
                is Resource.Success -> {
                    _unreadCount.value = result.data.unreadCount
                    _uiState.value     = NotificationsUiState(notifications = result.data.items)
                }
                is Resource.Error   -> _uiState.value = NotificationsUiState(error = result.message)
                Resource.Loading    -> _uiState.value = NotificationsUiState(isLoading = true)
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            if (repo.markRead(id) is Resource.Success) refresh()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            if (repo.markAllRead() is Resource.Success) refresh()
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount unread",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (unreadCount > 0) {
                        IconButton(onClick = viewModel::markAllRead) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Column(
                    modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = viewModel::refresh) { Text("Retry") }
                }
                uiState.notifications.isEmpty() -> Column(
                    modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "No notifications yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Push notifications and system alerts will appear here.",
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        style     = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(uiState.notifications, key = { it.id }) { n ->
                        NotificationItem(n, onClick = { viewModel.markRead(n.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: NotificationDto, onClick: () -> Unit) {
    val isUnread = !notification.isRead
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isUnread)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(if (isUnread) 2.dp else 1.dp),
    ) {
        Row(
            modifier  = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon indicator
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isUnread) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint     = if (isUnread) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        notification.title,
                        style    = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (isUnread) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp),
                        ) {}
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    notification.body,
                    style  = MaterialTheme.typography.bodySmall,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatVitalTimestamp(notification.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
