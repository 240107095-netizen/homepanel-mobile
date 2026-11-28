package com.example.homepanel.data.model

// Статус сериала на TVmaze
enum class ShowStatus { RUNNING, ENDED, TBD, UNKNOWN }

// Серия с датой выхода (airdate — «2026-12-04», как отдаёт TVmaze)
data class AirEpisode(
    val season: Int,
    val number: Int,
    val name: String,
    val airdate: String,
)

// Сериал из библиотеки + что о нём знает TVmaze: сколько серий уже вышло и когда следующая
data class ShowSchedule(
    val titleId: String,           // id в Jellyfin — по нему открываем экран тайтла
    val name: String,              // название как в библиотеке
    val year: Int,
    val posterUrl: String?,        // постер с TVmaze
    val network: String,           // канал или стриминг: «Netflix», «Tokyo MX»
    val status: ShowStatus,
    val airedEpisodes: Int,        // вышло серий (без спецвыпусков)
    val onServer: Int,             // лежит на сервере
    val lastAired: AirEpisode?,
    val next: AirEpisode?,         // null — дата следующей серии неизвестна
) {
    // Сколько вышедших серий ещё нет на сервере
    val missing: Int
        get() = (airedEpisodes - onServer).coerceAtLeast(0)
}
