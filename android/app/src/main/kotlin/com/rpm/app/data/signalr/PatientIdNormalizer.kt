package com.rpm.app.data.signalr

/**
 * Normalizes patient IDs for SignalR groups.
 * Use the patient's account GUID; short codes must be resolved server-side before subscribing.
 */
fun normalizePatientIdForHub(patientId: String): String {
    val trimmed = patientId.trim()
    if (trimmed.isEmpty()) return trimmed
    if (GUID_PATTERN.matches(trimmed)) return trimmed.lowercase()
    return trimmed.lowercase()
}

private val GUID_PATTERN =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

