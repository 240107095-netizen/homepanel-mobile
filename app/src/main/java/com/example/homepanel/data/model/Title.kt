package com.example.homepanel.data.model

// Тип тайтла в библиотеке
enum class TitleType { MOVIE, SERIES }

// Фильм или сериал из Jellyfin, уже готовый к показу
data class Title(
    val id: String,
    val type: TitleType,
    val name: String,              // русское название, как в Jellyfin
    val originalName: String,      // оригинальное, чтобы различать одноимённые
    val year: Int,
    val overview: String,
    val rating: Double?,           // оценка зрителей, null если нет
    val played: Boolean,
    val playedPercent: Int,        // фильм: сколько досмотрели, 0..100
    val episodeCount: Int,         // сериал: сколько серий есть
    val unplayedCount: Int,        // сериал: сколько не смотрели
    val hasPoster: Boolean,
)

// Сезон сериала
data class Season(
    val id: String,
    val seriesId: String,
    val number: Int,
    val name: String,
    val episodeCount: Int,
    val unplayedCount: Int,
)

// Серия сезона
data class Episode(
    val id: String,
    val season: Int,
    val number: Int,
    val name: String,
    val played: Boolean,
    val runtimeMinutes: Int,
)
