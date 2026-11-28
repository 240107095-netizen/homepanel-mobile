package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.JobStatus
import com.example.homepanel.data.model.OnlineDetails
import com.example.homepanel.data.model.OnlineJob
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

// Задача из GET api/jobs. Прогресс приходит в процентах 0..100.
@Serializable
data class MdlJobInfo(
    val id: String,
    val title: String = "",
    val season: Int = 1,
    val dub: String = "",
    val status: String = "queued",
    val progress: Double = 0.0,
    @SerialName("error_msg") val error: String? = null,
    @SerialName("created_at") val createdAt: Double = 0.0,
    @SerialName("target_library") val library: String = "",
    val items: List<MdlJobItem> = emptyList(),
)

// Одна серия задачи (ep_num = 0 — это фильм)
@Serializable
data class MdlJobItem(
    @SerialName("ep_num") val episode: Int = 0,
    val status: String = "queued",
)

fun MdlJobInfo.toModel() = OnlineJob(
    id = id,
    title = title,
    season = season,
    dub = dub.trim(),
    isFilm = library == "movies" || items.map { it.episode } == listOf(0),
    status = when (status) {
        "running", "downloading" -> JobStatus.RUNNING
        "done" -> JobStatus.DONE
        "failed" -> JobStatus.FAILED
        else -> JobStatus.QUEUED
    },
    progress = (progress / 100).toFloat().coerceIn(0f, 1f),
    doneEpisodes = items.count { it.status == "done" },
    totalEpisodes = items.size,
    error = error.orEmpty(),
    createdAt = createdAt,
)

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
