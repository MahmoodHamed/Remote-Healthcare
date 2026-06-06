package com.rpm.app.ui.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.ConversationDto
import com.rpm.app.data.remote.dto.MessageDto
import kotlinx.coroutines.launch

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
                    "No conversations.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
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
    ListItem(
        headlineContent = {
            Text(conv.title ?: conv.participants.joinToString { it.fullName })
        },
        supportingContent = {
            conv.lastMessageAt?.let { Text(it, maxLines = 1) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    onBack: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(input)
                        input = ""
                        scope.launch {
                            if (uiState.messages.isNotEmpty()) {
                                listState.animateScrollToItem(uiState.messages.size - 1)
                            }
                        }
                    },
                    enabled = !uiState.isSending && input.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
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
private fun MessageBubble(msg: MessageDto) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            msg.senderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(msg.content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Text(
            msg.sentAt,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
