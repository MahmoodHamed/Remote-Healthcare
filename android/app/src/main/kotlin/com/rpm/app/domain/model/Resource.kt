package com.rpm.app.domain.model

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

val <T> Resource<T>.isLoading get() = this is Resource.Loading
val <T> Resource<T>.isSuccess get() = this is Resource.Success
val <T> Resource<T>.isError   get() = this is Resource.Error
fun <T> Resource<T>.dataOrNull() = (this as? Resource.Success)?.data
