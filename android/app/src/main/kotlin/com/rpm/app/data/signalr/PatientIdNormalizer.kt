package com.rpm.app.data.signalr

import java.security.MessageDigest
import java.util.UUID

/**
 * Normalizes patient IDs for SignalR groups — matches web [normalizePatientId].
 * Accepts full GUID or 6-char short code (MD5 → UUID v4-style).
 */
fun normalizePatientIdForHub(patientId: String): String {
    val trimmed = patientId.trim()
    if (trimmed.isEmpty()) return trimmed
    if (GUID_PATTERN.matches(trimmed)) return trimmed.lowercase()
    if (SHORT_ID_PATTERN.matches(trimmed)) return shortIdToGuid(trimmed).lowercase()
    return trimmed.lowercase()
}

private val GUID_PATTERN =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

private val SHORT_ID_PATTERN = Regex("^[A-Za-z0-9]{6}$")

private fun shortIdToGuid(shortId: String): String = try {
    UUID.fromString(shortId).toString()
} catch (_: Exception) {
    val bytes = MessageDigest.getInstance("MD5").digest(shortId.toByteArray(Charsets.UTF_8))
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    buildString(36) {
        for (i in bytes.indices) {
            if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
            append("%02x".format(bytes[i].toInt() and 0xff))
        }
    }
}
