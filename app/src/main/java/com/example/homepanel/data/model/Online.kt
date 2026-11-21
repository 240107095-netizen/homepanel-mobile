package com.example.homepanel.data.model

// Онлайн-сайт, с которого MediaDL качает на сервер
enum class OnlineSite(val category: String, val source: String, val label: String) {
    REZKA("cinema", "hdrezka", "HDRezka"),
    ANIMEGO("anime", "animego", "AnimeGo"),
}

// Найденный на сайте тайтл. searchId — сессия поиска MediaDL (живёт ограниченное время).
data class OnlineResult(
    val searchId: String,
    val idx: Int,
    val site: OnlineSite,
    val title: String,
    val year: String,
    val posterUrl: String?,
)

// Что можно скачать: озвучки, качества, сезоны, серии
data class OnlineDetails(
    val title: String,
    val isFilm: Boolean,
    val seasons: List<Int>,
    val season: Int,
    val totalEpisodes: Int,
    val dubs: List<String>,
    val qualities: List<Int>,     // 1080, 720, 480
    val existing: List<Int>,      // серии этого сезона, что уже лежат на сервере
) {
    // По умолчанию качаем только то, чего ещё нет
    val missing: List<Int> get() = (1..totalEpisodes).filter { it !in existing }
}
