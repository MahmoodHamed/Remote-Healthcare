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
        403 -> "Access denied. You do not have permission for this action."
        404 -> "This feature is not available on the server yet. Deploy the latest API or sign in again."
        502 -> "Server unavailable (502). The API is not running on remote-care.tech."
        503 -> "Service unavailable (503). Try again shortly."
        else -> response.message().ifBlank { "Request failed (${response.code()})" }
    }
}
