package com.example.homepanel.ui

import androidx.annotation.StringRes

// Короткое сообщение внизу экрана (snackbar): строка + необязательный аргумент
data class UserMessage(@StringRes val text: Int, val arg: String? = null)
