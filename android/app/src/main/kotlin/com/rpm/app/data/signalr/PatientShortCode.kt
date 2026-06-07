package com.rpm.app.data.signalr

import java.security.MessageDigest
import java.util.UUID

/** 6-char patient code for watch pairing (matches backend PatientShortCode). */
object PatientShortCode {
    private const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val SHORT_PATTERN = Regex("^[A-Za-z0-9]{6}$")

    fun isValid(value: String): Boolean = SHORT_PATTERN.matches(value.trim())

    fun fromUserId(userId: String, salt: Int = 0): String {
        val normalized = runCatching { UUID.fromString(userId).toString() }.getOrElse { userId.lowercase() }
        val input = if (salt == 0) normalized else "$normalized:$salt"
        val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return buildString(6) {
            for (i in 0 until 6) append(CHARS[(hash[i].toInt() and 0xFF) % CHARS.length])
        }
    }
}
