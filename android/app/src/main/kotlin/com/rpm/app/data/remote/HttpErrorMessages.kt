package com.rpm.app.data.remote

import retrofit2.Response

fun httpErrorMessage(response: Response<*>): String {
    val body = response.errorBody()?.string()?.trim().orEmpty()
    if (body.isNotEmpty()) {
        Regex(""""message"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)?.let { return it }
        Regex(""""title"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)?.let { return it }
    }
    return when (response.code()) {
        401 -> "Session expired. Please sign in again."
        403 -> "Access denied. The My Patients screen requires a Doctor account. Sign out, register or log in as Doctor."
        502 -> "Server unavailable (502). The API is not running on remote-care.tech."
        503 -> "Service unavailable (503). Try again shortly."
        else -> response.message().ifBlank { "Request failed (${response.code()})" }
    }
}
