package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.MediaStatus
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.TitleInfo
import com.example.homepanel.data.model.TitleType
import kotlinx.serialization.Serializable

@Serializable
data class SeerrSearchPage(val results: List<SeerrResult> = emptyList())

// Результат поиска: у фильма title/releaseDate, у сериала name/firstAirDate
@Serializable
data class SeerrResult(
    val id: Int,
    val mediaType: String = "",
    val title: String? = null,
    val name: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val mediaInfo: SeerrMediaInfo? = null,
)

// status: 1 неизвестно, 2 ждёт одобрения, 3 качается, 4 частично есть, 5 есть
@Serializable
data class SeerrMediaInfo(val status: Int = 1)

// Подробности тайтла (/movie/{id} или /tv/{id})
@Serializable
data class SeerrDetails(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val originalTitle: String? = null,
    val originalName: String? = null,
    val originalLanguage: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val genres: List<SeerrGenre> = emptyList(),
    val externalIds: SeerrExternalIds? = null,
)

@Serializable
data class SeerrGenre(val id: Int)

@Serializable
data class SeerrExternalIds(val tvdbId: Int? = null)

private const val ANIMATION_GENRE = 16

// en — ответ на английском, local — на языке интерфейса (для онлайн-сайтов)
fun detailsToTitleInfo(en: SeerrDetails, local: SeerrDetails, type: TitleType): TitleInfo {
    val date = en.releaseDate ?: en.firstAirDate
    return TitleInfo(
        tmdbId = en.id,
        type = type,
        name = local.title ?: local.name ?: "",
        englishName = en.title ?: en.name ?: "",
        originalName = en.originalTitle ?: en.originalName ?: "",
        year = date?.take(4)?.toIntOrNull(),
        isAnime = en.originalLanguage == "ja" && en.genres.any { it.id == ANIMATION_GENRE },
        tvdbId = en.externalIds?.tvdbId,
    )
}

@Serializable
data class SeerrStatus(val version: String = "")

// Тело POST /api/v1/request. Для сериала просим все сезоны.
@Serializable
data class SeerrRequestBody(
    val mediaType: String,
    val mediaId: Int,
    val seasons: String? = null,
)

private const val TMDB_POSTER = "https://image.tmdb.org/t/p/w185"

// Люди (актёры) в поиске нам не нужны — для них null
fun SeerrResult.toSearchResult(): SearchResult? {
    val type = when (mediaType) {
        "movie" -> TitleType.MOVIE
        "tv" -> TitleType.SERIES
        else -> return null
    }
    val date = releaseDate ?: firstAirDate
    return SearchResult(
        tmdbId = id,
        type = type,
        name = title ?: name ?: "",
        year = date?.take(4)?.toIntOrNull(),
        overview = overview.orEmpty(),
        posterUrl = posterPath?.let { TMDB_POSTER + it },
        status = when (mediaInfo?.status) {
            4, 5 -> MediaStatus.AVAILABLE
            2, 3 -> MediaStatus.REQUESTED
            else -> MediaStatus.NONE
        },
    )
}
