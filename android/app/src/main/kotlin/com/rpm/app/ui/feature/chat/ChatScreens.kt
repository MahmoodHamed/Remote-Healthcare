package com.rpm.app.ui.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.ConversationDto
import com.rpm.app.data.remote.dto.MessageDto

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
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.conversations.isEmpty() -> Text(
                    "No conversations yet.\nStart a chat from a patient profile.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.conversations, key = { it.id }) { c ->
                        ConversationItem(c, onClick = { onConversationClick(c.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(conv: ConversationDto, onClick: () -> Unit) {
    val title = conv.title ?: conv.participants.joinToString { it.fullName }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                conv.lastMessageAt?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        ChatMessageUtils.formatTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    onBack: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val listItems = remember(uiState.messages) { buildChatListItems(uiState.messages) }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(listItems.size) {
        if (listItems.isNotEmpty()) {
            listState.animateScrollToItem(listItems.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.conversationTitle, maxLines = 1)
                        Text(
                            "Secure care chat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        enabled = !uiState.isSending && input.isNotBlank(),
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                listItems.isEmpty() -> Text(
                    "No messages yet.\nSay hello to start the conversation.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(listItems, key = { item ->
                        when (item) {
                            is ChatListItem.DaySeparator -> "day-${item.key}"
                            is ChatListItem.Message -> item.message.id
                        }
                    }) { item ->
                        when (item) {
                            is ChatListItem.DaySeparator -> DaySeparatorChip(item.label)
                            is ChatListItem.Message -> MessageBubble(
                                message = item.message,
                                isMine = item.message.senderId.equals(currentUserId, ignoreCase = true),
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, isMine: Boolean) {
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        if (!isMine) {
            Text(
                message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.content, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    ChatMessageUtils.formatTime(message.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
