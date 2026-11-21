package com.example.homepanel.ui

import androidx.annotation.StringRes
import com.example.homepanel.R
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

// Исключение -> понятный текст для экрана ошибки (без трейсбеков).
// fallback — что сказать, если причина неизвестна («Не удалось загрузить библиотеку»).
@StringRes
fun Throwable.toErrorMessage(@StringRes fallback: Int): Int = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> R.string.error_auth
        in 500..599 -> R.string.error_server
        else -> fallback
    }
    is SerializationException -> R.string.error_bad_response
    is IOException -> R.string.error_no_connection   // таймаут, нет сети, сервер выключен
    else -> fallback
}
