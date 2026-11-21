package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.OnlineDetails
import com.example.homepanel.data.model.OnlineResult
import com.example.homepanel.data.model.OnlineSite
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MdlSearch(
    @SerialName("search_id") val searchId: String = "",
    val results: List<MdlFound> = emptyList(),
    val error: String? = null,
)

@Serializable
data class MdlFound(
    val idx: Int,
    val title: String = "",
    val year: String? = null,
    val poster: String? = null,
)

@Serializable
data class MdlDetails(
    val title: String = "",
    @SerialName("clean_title") val cleanTitle: String? = null,
    val type: String = "",
    val seasons: List<Int> = emptyList(),
    @SerialName("default_season") val defaultSeason: Int? = null,
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    val dubs: List<String> = emptyList(),
    val qualities: List<Int> = emptyList(),
    @SerialName("existing_episodes") val existing: List<Int> = emptyList(),
    val error: String? = null,
)

@Serializable
data class MdlJobBody(
    @SerialName("search_id") val searchId: String,
    val idx: Int,
    @SerialName("media_type") val mediaType: String,
    val dub: String,
    val quality: String,
    val season: Int,
    val episodes: List<Int>,
    @SerialName("show_title") val showTitle: String,
)

@Serializable
data class MdlJob(@SerialName("job_id") val jobId: String = "")

fun MdlFound.toModel(searchId: String, site: OnlineSite) = OnlineResult(
    searchId = searchId, idx = idx, site = site, title = title, year = year.orEmpty(), posterUrl = poster,
)

fun MdlDetails.toModel(): OnlineDetails {
    val s = seasons.ifEmpty { listOf(1) }
    return OnlineDetails(
        title = cleanTitle ?: title,
        isFilm = type == "film",
        seasons = s,
        season = defaultSeason ?: s.first(),
        totalEpisodes = totalEpisodes,
        dubs = dubs.map { it.trim() }.ifEmpty { listOf("") },
        qualities = qualities.ifEmpty { listOf(1080, 720, 480) },
        existing = existing.sorted(),
    )
}
