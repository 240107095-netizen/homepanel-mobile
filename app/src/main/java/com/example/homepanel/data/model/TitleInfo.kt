package com.example.homepanel.data.model

// Подробности найденного тайтла, нужные для поиска раздач и скачивания
data class TitleInfo(
    val tmdbId: Int,
    val type: TitleType,
    val name: String,          // на языке интерфейса (для онлайн-сайтов — русское)
    val englishName: String,   // для трекеров: в названиях раздач почти всегда английское
    val originalName: String,
    val year: Int?,
    val isAnime: Boolean,      // японская анимация: свои трекеры, папка и AnimeGo
    val tvdbId: Int?,          // Sonarr знает сериалы по TVDB id
) {
    // «Dune 2021» — у фильмов год сильно сужает поиск
    val trackerQuery: String
        get() {
            val base = englishName.ifBlank { originalName.ifBlank { name } }
            return if (type == TitleType.MOVIE && year != null) "$base $year" else base
        }

    // «Mushoku Tensei: Jobless Reincarnation» -> «Mushoku Tensei»: многие раздачи без подзаголовка
    val shortQuery: String?
        get() {
            val head = trackerQuery.substringBefore(':').trim()
            if (head == trackerQuery || head.length < 4) return null
            return if (type == TitleType.MOVIE && year != null) "$head $year" else head
        }
}
