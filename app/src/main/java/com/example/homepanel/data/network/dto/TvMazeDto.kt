package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.AirEpisode
import com.example.homepanel.data.model.ShowSchedule
import com.example.homepanel.data.model.ShowStatus
import com.example.homepanel.data.model.Title
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Ответ TVmaze /singlesearch/shows (только нужные поля, остальные пропускаются)
@Serializable
data class TvMazeShow(
    val id: Int,
    val name: String = "",
    val premiered: String? = null,
    val status: String = "",
    val network: TvMazeChannel? = null,       // телеканал
    val webChannel: TvMazeChannel? = null,    // или стриминг
    val image: TvMazeImage? = null,
    @SerialName("_embedded") val embedded: TvMazeEmbedded? = null,
)

@Serializable
data class TvMazeChannel(val name: String = "")

@Serializable
data class TvMazeImage(val medium: String? = null, val original: String? = null)

@Serializable
data class TvMazeEmbedded(
    val nextepisode: TvMazeEpisode? = null,
    val episodes: List<TvMazeEpisode> = emptyList(),
)

// number == null и type != regular — спецвыпуски, их не считаем
@Serializable
data class TvMazeEpisode(
    val season: Int = 0,
    val number: Int? = null,
    val name: String = "",
    val airdate: String = "",
    val type: String = "regular",
)

private fun TvMazeEpisode.toModel(): AirEpisode? =
    number?.let { AirEpisode(season, it, name, airdate) }

// serverSeasons — номер сезона → сколько серий лежит на сервере.
// today — «yyyy-MM-dd»: даты в ISO-формате можно сравнивать как строки.
fun TvMazeShow.toSchedule(title: Title, serverSeasons: Map<Int, Int>, today: String): ShowSchedule {
    val aired = embedded?.episodes.orEmpty()
        .filter { it.type == "regular" && it.number != null && it.airdate.isNotEmpty() && it.airdate <= today }
    val airedBySeason = aired.groupBy { it.season }.mapValues { it.value.size }
    val have = serverSeasons.filterKeys { it > 0 }   // сезон 0 в Jellyfin — спецвыпуски
    val lastHave = have.keys.maxOrNull() ?: 0
    val newer = airedBySeason.filterKeys { it > lastHave }
    return ShowSchedule(
        titleId = title.id,
        name = title.name,
        year = title.year,
        posterUrl = image?.medium ?: title.posterUrl,
        network = network?.name ?: webChannel?.name.orEmpty(),
        status = when (status) {
            "Running" -> ShowStatus.RUNNING
            "Ended" -> ShowStatus.ENDED
            "To Be Determined", "In Development" -> ShowStatus.TBD
            else -> ShowStatus.UNKNOWN
        },
        airedEpisodes = have.keys.sumOf { airedBySeason[it] ?: 0 },
        onServer = have.values.sum(),
        missing = have.entries.sumOf { (season, count) -> ((airedBySeason[season] ?: 0) - count).coerceAtLeast(0) },
        newSeasons = newer.keys.sorted(),
        newSeasonEpisodes = newer.values.sum(),
        lastAired = aired.lastOrNull()?.toModel(),
        next = embedded?.nextepisode?.toModel(),
    )
}
