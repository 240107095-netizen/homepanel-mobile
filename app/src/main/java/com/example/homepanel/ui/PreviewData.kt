package com.example.homepanel.ui

import com.example.homepanel.data.model.MediaStatus
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.ServiceStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.model.TorrentState

// Образцовые данные только для @Preview в Android Studio
object PreviewData {
    val titles = listOf(
        Title("1", TitleType.SERIES, "Cyberpunk: Edgerunners", "", 2022, "Street kid David…", 8.6,
            played = true, playedPercent = 100, episodeCount = 10, unplayedCount = 0, posterUrl = null),
        Title("2", TitleType.MOVIE, "Дюна: Часть вторая", "Dune: Part Two", 2024, "Paul Atreides…", 8.1,
            played = false, playedPercent = 54, episodeCount = 0, unplayedCount = 0, posterUrl = null),
        Title("3", TitleType.SERIES, "Shōgun", "", 2024, "", 8.6,
            played = false, playedPercent = 0, episodeCount = 10, unplayedCount = 10, posterUrl = null),
    )

    val seasons = listOf(
        Season("s1", "1", 1, "Season 1", 12, 0),
        Season("s2", "1", 2, "Season 2", 13, 9),
    )

    val torrents = listOf(
        Torrent("a", "Dune.Part.Two.2024.2160p.WEB-DL", 18_400_000_000, 0.54f, 3_100_000, 2_900, TorrentState.DOWNLOADING),
        Torrent("b", "Shogun.S01.1080p.WEB-DL", 21_700_000_000, 0.18f, 0, 0, TorrentState.PAUSED),
        Torrent("c", "Solo Leveling S02 1080p", 10_000_000_000, 1f, 0, 0, TorrentState.DONE),
    )

    val searchResults = listOf(
        SearchResult(1, TitleType.MOVIE, "Dune", 2021, "", null, MediaStatus.AVAILABLE),
        SearchResult(2, TitleType.SERIES, "Dune: Prophecy", 2024, "", null, MediaStatus.NONE),
    )

    val server = ServerStatus(
        "debian", 144_778_027_008, 310_831_661_056,
        listOf(ServiceStatus("Jellyfin", true, "12.3.0"), ServiceStatus("Jellyseerr", false, null)),
    )
}
