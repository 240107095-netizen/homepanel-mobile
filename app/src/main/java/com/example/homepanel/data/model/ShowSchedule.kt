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

// Сериал из библиотеки + что о нём знает TVmaze. Сравниваем по сезонам, которые уже есть
// на сервере: ранние сезоны, которые не качали, не считаются «пропущенными».
data class ShowSchedule(
    val titleId: String,           // id в Jellyfin — по нему открываем экран тайтла
    val name: String,              // название как в библиотеке
    val year: Int,
    val posterUrl: String?,        // постер с TVmaze
    val network: String,           // канал или стриминг: «Netflix», «Tokyo MX»
    val status: ShowStatus,
    val airedEpisodes: Int,        // вышло серий в тех сезонах, что есть на сервере (без спецвыпусков)
    val onServer: Int,             // лежит на сервере в этих сезонах
    val missing: Int,              // вышли, но на сервере их нет
    val newSeasons: List<Int>,     // сезоны после последнего скачанного, которые уже вышли
    val newSeasonEpisodes: Int,    // сколько в них серий
    val lastAired: AirEpisode?,
    val next: AirEpisode?,         // null — дата следующей серии неизвестна
) {
    val isBehind: Boolean
        get() = missing > 0 || newSeasons.isNotEmpty()
}
