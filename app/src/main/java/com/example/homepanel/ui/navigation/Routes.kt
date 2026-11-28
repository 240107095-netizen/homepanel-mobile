package com.example.homepanel.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import com.example.homepanel.R

// Все маршруты приложения в одном месте. Аргументы — в фигурных скобках.
object Routes {
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val SAVED = "saved"
    const val SERVER = "server"

    // Новые серии сериалов из библиотеки (данные из публичного TVmaze)
    const val UPCOMING = "upcoming"

    // Экран тайтла: id из Jellyfin передаётся аргументом маршрута
    const val ARG_ITEM_ID = "itemId"
    const val TITLE = "title/{$ARG_ITEM_ID}"
    fun title(itemId: String) = "title/$itemId"

    // Экран сезона: два аргумента — сериал и сезон
    const val ARG_SERIES_ID = "seriesId"
    const val ARG_SEASON_ID = "seasonId"
    const val SEASON = "season/{$ARG_SERIES_ID}/{$ARG_SEASON_ID}"
    fun season(seriesId: String, seasonId: String) = "season/$seriesId/$seasonId"

    // Источники для найденного тайтла: тип и TMDB id — обязательные (id — Int),
    // название — необязательный query-аргумент, чтобы шапка была видна сразу
    const val ARG_TYPE = "type"
    const val ARG_TMDB_ID = "tmdbId"
    const val ARG_TITLE = "title"
    const val SOURCES = "sources/{$ARG_TYPE}/{$ARG_TMDB_ID}?$ARG_TITLE={$ARG_TITLE}"
    fun sources(type: String, tmdbId: Int, title: String) = "sources/$type/$tmdbId?$ARG_TITLE=${Uri.encode(title)}"
}

// Вкладки нижней панели: маршрут, подпись и текстовый значок
enum class TopTab(val route: String, @StringRes val label: Int, val glyph: String) {
    LIBRARY(Routes.LIBRARY, R.string.tab_library, "≡"),
    DOWNLOADS(Routes.DOWNLOADS, R.string.tab_downloads, "↓"),
    SEARCH(Routes.SEARCH, R.string.tab_search, "/"),
    SAVED(Routes.SAVED, R.string.tab_saved, "★"),
    SERVER(Routes.SERVER, R.string.tab_server, "◉"),
}
