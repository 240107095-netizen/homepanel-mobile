package com.example.homepanel.ui.navigation

import androidx.annotation.StringRes
import com.example.homepanel.R

// Все маршруты приложения в одном месте. Аргументы — в фигурных скобках.
object Routes {
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val SERVER = "server"

    // Экран тайтла: id из Jellyfin передаётся аргументом маршрута
    const val ARG_ITEM_ID = "itemId"
    const val TITLE = "title/{$ARG_ITEM_ID}"
    fun title(itemId: String) = "title/$itemId"
}

// Вкладки нижней панели: маршрут, подпись и текстовый значок
enum class TopTab(val route: String, @StringRes val label: Int, val glyph: String) {
    LIBRARY(Routes.LIBRARY, R.string.tab_library, "≡"),
    DOWNLOADS(Routes.DOWNLOADS, R.string.tab_downloads, "↓"),
    SEARCH(Routes.SEARCH, R.string.tab_search, "/"),
    SERVER(Routes.SERVER, R.string.tab_server, "◉"),
}
