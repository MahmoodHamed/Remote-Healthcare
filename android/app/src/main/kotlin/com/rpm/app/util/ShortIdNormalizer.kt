package com.rpm.app.util

import java.security.MessageDigest

/**
 * Converts a 6-char alphanumeric watch short ID (e.g. "ABC123") into a deterministic UUID.
 * Algorithm is identical to the web `normalizePatientId()` in `patientId.js`:
 *   1. Uppercase the input
 *   2. MD5 hash it
 *   3. Set version bits: bytes[6] = (b & 0x0f) | 0x30, bytes[8] = (b & 0x3f) | 0x80
 *   4. Format as UUID string
 */
object ShortIdNormalizer {

    private val SHORT_ID_REGEX = Regex("^[A-Za-z0-9]{6}$")
    private val GUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** Returns the streaming patient UUID for the given value, or null if invalid. */
    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (GUID_REGEX.matches(trimmed)) return trimmed.lowercase()
        if (!SHORT_ID_REGEX.matches(trimmed)) return null

        val shortId = trimmed.uppercase()
        val bytes = MessageDigest.getInstance("MD5")
            .digest(shortId.toByteArray(Charsets.UTF_8))
            .copyOf(16)

        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()

        return buildString {
            bytes.forEachIndexed { i, b ->
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                append(b.toInt().and(0xff).toString(16).padStart(2, '0'))
            }
        }
    }

    fun isValidShortId(value: String) = SHORT_ID_REGEX.matches(value.trim())
}
