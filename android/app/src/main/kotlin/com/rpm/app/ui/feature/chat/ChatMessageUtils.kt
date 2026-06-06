package com.rpm.app.ui.feature.chat

import com.rpm.app.data.remote.dto.MessageDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object ChatMessageUtils {

    fun sortAscending(messages: List<MessageDto>): List<MessageDto> =
        messages.sortedWith(
            compareBy<MessageDto>({ parseInstant(it.sentAt) ?: Instant.EPOCH }, { it.id.lowercase() }),
        )

    fun parseInstant(sentAt: String): Instant? = runCatching {
        Instant.parse(sentAt)
    }.getOrNull()

    fun formatTime(sentAt: String): String {
        val instant = parseInstant(sentAt) ?: return sentAt
        val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    }

    fun formatDayLabel(sentAt: String): String {
        val instant = parseInstant(sentAt) ?: return sentAt
        val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
        val today = LocalDateTime.now(ZoneId.systemDefault()).toLocalDate()
        return when (local) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> local.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
        }
    }

    fun dayKey(sentAt: String): String {
        val instant = parseInstant(sentAt) ?: return sentAt
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate().toString()
    }
}

sealed class ChatListItem {
    data class DaySeparator(val label: String, val key: String) : ChatListItem()
    data class Message(val message: MessageDto) : ChatListItem()
}

fun buildChatListItems(messages: List<MessageDto>): List<ChatListItem> {
    val sorted = ChatMessageUtils.sortAscending(messages)
    val items = mutableListOf<ChatListItem>()
    var lastDay: String? = null
    for (message in sorted) {
        val day = ChatMessageUtils.dayKey(message.sentAt)
        if (day != lastDay) {
            items += ChatListItem.DaySeparator(ChatMessageUtils.formatDayLabel(message.sentAt), day)
            lastDay = day
        }
        items += ChatListItem.Message(message)
    }
    return items
}
