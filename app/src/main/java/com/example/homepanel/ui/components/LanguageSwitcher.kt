package com.example.homepanel.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.example.homepanel.R

// Язык приложения: «[System] English · Русский · Қазақша».
// AppCompat сохраняет выбор и пересоздаёт экран — все stringResource() сразу берут
// строки из values-ru / values-kk. Новый язык = новая папка values-xx, код не меняется.
private val languageTags = listOf("", "en", "ru", "kk")  // "" — как в системе

@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    TuiTabs(
        labels = listOf(
            stringResource(R.string.lang_system),
            stringResource(R.string.lang_en),
            stringResource(R.string.lang_ru),
            stringResource(R.string.lang_kk),
        ),
        selected = languageTags.indexOf(current).coerceAtLeast(0),
        onSelect = { i ->
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTags[i]))
        },
        modifier = modifier,
    )
}
