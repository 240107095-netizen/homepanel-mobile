package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.Episode
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Ответы Jellyfin как есть (поля с большой буквы). Лишние поля игнорируются.

@Serializable
data class JfUser(@SerialName("Id") val id: String)

@Serializable
data class JfItems(@SerialName("Items") val items: List<JfItem> = emptyList())

@Serializable
data class JfItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("OriginalTitle") val originalTitle: String? = null,
    @SerialName("ProductionYear") val year: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("CommunityRating") val rating: Double? = null,
    @SerialName("RecursiveItemCount") val recursiveItemCount: Int? = null,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("UserData") val userData: JfUserData? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
)

@Serializable
data class JfUserData(
    @SerialName("Played") val played: Boolean = false,
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerialName("UnplayedItemCount") val unplayedItemCount: Int? = null,
)

@Serializable
data class JfSystemInfo(
    @SerialName("ServerName") val serverName: String = "",
    @SerialName("Version") val version: String = "",
)

// --- перевод в модели приложения ---

// 1 тик Jellyfin = 100 нс, в минуте 600 000 000 тиков
private const val TICKS_PER_MINUTE = 600_000_000L

fun JfItem.toTitle(): Title {
    val isSeries = type == "Series"
    val ud = userData
    return Title(
        id = id,
        type = if (isSeries) TitleType.SERIES else TitleType.MOVIE,
        name = name,
        originalName = originalTitle.orEmpty(),
        year = year ?: 0,
        overview = overview.orEmpty(),
        rating = rating,
        played = ud?.played == true,
        playedPercent = if (ud?.played == true) 100 else (ud?.playedPercentage ?: 0.0).toInt(),
        episodeCount = if (isSeries) recursiveItemCount ?: 0 else 0,
        unplayedCount = if (isSeries) ud?.unplayedItemCount ?: 0 else 0,
        hasPoster = "Primary" in imageTags,
    )
}

fun JfItem.toSeason(seriesId: String) = Season(
    id = id,
    seriesId = seriesId,
    number = indexNumber ?: 0,
    name = name,
    episodeCount = childCount ?: recursiveItemCount ?: 0,
    unplayedCount = userData?.unplayedItemCount ?: 0,
)

fun JfItem.toEpisode() = Episode(
    id = id,
    season = parentIndexNumber ?: 0,
    number = indexNumber ?: 0,
    name = name,
    played = userData?.played == true,
    runtimeMinutes = ((runTimeTicks ?: 0L) / TICKS_PER_MINUTE).toInt(),
)
