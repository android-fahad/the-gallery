package com.polylab.thegallery.core.result

import com.polylab.thegallery.core.result.GResult

sealed class GResult<out T> {
    data class Success<T>(val data: T) : GResult<T>()
    data class Error(val exception: Throwable) : GResult<Nothing>()
    data object Loading : GResult<Nothing>()
}

inline fun <T> GResult<T>.onSuccess(action: (value: T) -> Unit): GResult<T> {
    if (this is GResult.Success) action(data)
    return this
}

inline fun <T> GResult<T>.onError(action: (exception: Throwable) -> Unit): GResult<T> {
    if (this is GResult.Error) action(exception)
    return this
}