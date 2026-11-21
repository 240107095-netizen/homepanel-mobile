package com.example.homepanel.data.model

// Статус тайтла на сервере: есть, запрошен (ищется) или ещё нет
enum class MediaStatus { AVAILABLE, REQUESTED, NONE }

// Найденный тайтл из TMDB (через Jellyseerr)
data class SearchResult(
    val tmdbId: Int,
    val type: TitleType,
    val name: String,
    val year: Int?,
    val overview: String,
    val posterUrl: String?,
    val status: MediaStatus,
)
