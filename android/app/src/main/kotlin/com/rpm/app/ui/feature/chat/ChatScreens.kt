package com.rpm.app.ui.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.ConversationDto
import com.rpm.app.data.remote.dto.MessageDto
import com.rpm.app.ui.feature.patients.formatVitalTimestamp

// ── Conversation List Screen ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (conversationId: String) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar   = {
            TopAppBar(
                title = {
                    Column {
                        Text("Messages", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        if (!uiState.isLoading && uiState.conversations.isNotEmpty()) {
                            Text(
                                "${uiState.conversations.size} conversation${if (uiState.conversations.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    IconButton(onClick = viewModel::loadConversations) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                    OutlinedButton(onClick = viewModel::loadConversations) { Text("Retry") }
                }
                uiState.conversations.isEmpty() -> EmptyConversationsState(Modifier.align(Alignment.Center))
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.conversations, key = { it.id }) { c ->
                        ConversationItem(c, onClick = { onConversationClick(c.id) })
                        HorizontalDivider(
                            modifier    = Modifier.padding(start = 80.dp, end = 16.dp),
                            color       = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationsState(modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier.padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape  = CircleShape,
            color  = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Text(
            "No conversations yet",
            style  = MaterialTheme.typography.titleMedium,
            color  = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Conversations with your care team will appear here.\nOpen a patient profile to start a chat.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationItem(conv: ConversationDto, onClick: () -> Unit) {
    val title    = conv.title ?: conv.participants.joinToString { it.fullName }
    val initials = title.take(2).uppercase()

    Row(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                conv.lastMessageAt?.let {
                    Text(
                        formatVitalTimestamp(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    when (conv.type) {
                        "DoctorPatient" -> "Care conversation"
                        "Group"         -> "Group chat"
                        else            -> "Secure chat"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Chat Room Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    onBack: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val listItems     = remember(uiState.messages) { buildChatListItems(uiState.messages) }
    val listState     = rememberLazyListState()
    var input         by remember { mutableStateOf("") }

    LaunchedEffect(listItems.size) {
        if (listItems.isNotEmpty()) listState.animateScrollToItem(listItems.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar circle
                        val initials = uiState.conversationTitle.take(2).uppercase()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initials,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                uiState.conversationTitle,
                                maxLines = 1,
                                style    = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Secure care chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value         = input,
                        onValueChange = { input = it },
                        modifier      = Modifier.weight(1f),
                        placeholder   = { Text("Type a message…") },
                        shape         = RoundedCornerShape(24.dp),
                        maxLines      = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick  = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        enabled  = !uiState.isSending && input.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                listItems.isEmpty() -> Column(
                    modifier              = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "No messages yet\nSay hello to start the conversation!",
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    state           = listState,
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(listItems, key = { item ->
                        when (item) {
                            is ChatListItem.DaySeparator -> "day-${item.key}"
                            is ChatListItem.Message      -> item.message.id
                        }
                    }) { item ->
                        when (item) {
                            is ChatListItem.DaySeparator -> DaySeparatorChip(item.label)
                            is ChatListItem.Message      -> MessageBubble(
                                message = item.message,
                                isMine  = item.message.senderId.equals(currentUserId, ignoreCase = true),
                            )
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                ) { Text(error) }
            }
        }
    }
}

@Composable
private fun DaySeparatorChip(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, isMine: Boolean) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary
                     else         MaterialTheme.colorScheme.surfaceVariant
    val textColor   = if (isMine) MaterialTheme.colorScheme.onPrimary
                     else         MaterialTheme.colorScheme.onSurfaceVariant
    val shape       = RoundedCornerShape(
        topStart    = 18.dp,
        topEnd      = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd   = if (isMine) 4.dp  else 18.dp,
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMine) 48.dp else 0.dp,
                end   = if (isMine) 0.dp  else 48.dp,
            ),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        if (!isMine) {
            Text(
                message.senderName,
                style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape    = shape,
            color    = bubbleColor,
            modifier = Modifier.widthIn(min = 60.dp),
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.content, color = textColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.align(Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        ChatMessageUtils.formatTime(message.sentAt),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = textColor.copy(alpha = 0.7f),
                    )
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint     = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
