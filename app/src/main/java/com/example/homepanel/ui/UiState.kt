package com.example.homepanel.ui

import androidx.annotation.StringRes

// Одно из состояний любого экрана: грузится, ошибка или данные.
// Ошибка хранит id строки, чтобы текст переводился вместе с интерфейсом.
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(@StringRes val message: Int) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}
