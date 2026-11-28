package com.example.homepanel.data

import com.example.homepanel.data.model.Episode
import com.example.homepanel.data.model.JobStatus
import com.example.homepanel.data.model.MediaStatus
import com.example.homepanel.data.model.OnlineJob
import com.example.homepanel.data.model.OnlineDetails
import com.example.homepanel.data.model.OnlineResult
import com.example.homepanel.data.model.OnlineSite
import com.example.homepanel.data.model.QualityProfile
import com.example.homepanel.data.model.Release
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.ServiceStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleInfo
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.model.TorrentState
import kotlinx.coroutines.delay

// Демо-данные: для превью и для запуска без ключей сервера.
// delay() имитирует сеть, чтобы было видно состояние загрузки.
class FakeMediaRepository(private val latencyMs: Long = 400) : MediaRepository {

    private val titles = listOf(
        title("m1", TitleType.MOVIE, "Человек-паук: Новый день", "Spider-Man: Brand New Day", 2026, 8.5,
            played = false, percent = 54),
        title("m2", TitleType.MOVIE, "Дюна: Часть вторая", "Dune: Part Two", 2024, 8.1, played = true),
        title("m3", TitleType.MOVIE, "Интерстеллар", "Interstellar", 2014, 8.7, played = true),
        title("s1", TitleType.SERIES, "Киберпанк: Бегущие по краю", "Cyberpunk: Edgerunners", 2022, 8.6,
            played = true, episodes = 10),
        title("s2", TitleType.SERIES, "Поднятие уровня в одиночку", "Solo Leveling", 2024, 8.3,
            played = false, episodes = 25, unplayed = 13),
        title("s3", TitleType.SERIES, "Шогун", "Shōgun", 2024, 8.6, played = false, episodes = 10, unplayed = 10),
    )

    private val seasons = mapOf(
        "s1" to listOf(Season("s1-1", "s1", 1, "Сезон 1", 10, 0)),
        "s2" to listOf(Season("s2-1", "s2", 1, "Сезон 1", 12, 0), Season("s2-2", "s2", 2, "Сезон 2", 13, 13)),
        "s3" to listOf(Season("s3-1", "s3", 1, "Сезон 1", 10, 10)),
    )

    // Изменяемый список: пауза/старт меняют состояние, как на настоящем сервере
    private val torrents = mutableListOf(
        Torrent("h1", "Spider-Man.Brand.New.Day.2026.2160p.WEB-DL", 18_400_000_000, 0.54f,
            3_100_000, 2_900, TorrentState.DOWNLOADING),
        Torrent("h2", "Solo Leveling - S02 (Bluray-1080p AV1)", 10_025_609_032, 1f, 0, 0, TorrentState.DONE),
        Torrent("h3", "Shogun.S01.1080p.WEB-DL", 21_700_000_000, 0.18f, 0, 0, TorrentState.PAUSED),
    )

    override suspend fun getLibrary(): List<Title> {
        delay(latencyMs)
        return titles
    }

    override suspend fun getTitle(id: String): Title {
        delay(latencyMs)
        return titles.first { it.id == id }
    }

    override suspend fun getSeasons(seriesId: String): List<Season> {
        delay(latencyMs)
        return seasons[seriesId].orEmpty()
    }

    override suspend fun getEpisodes(seriesId: String, seasonId: String): List<Episode> {
        delay(latencyMs)
        val season = seasons[seriesId].orEmpty().first { it.id == seasonId }
        val watched = season.episodeCount - season.unplayedCount
        return (1..season.episodeCount).map { n ->
            Episode("$seasonId-e$n", season.number, n, "Серия $n", played = n <= watched, runtimeMinutes = 24)
        }
    }

    override suspend fun getTorrents(): List<Torrent> {
        delay(latencyMs)
        return torrents.toList()
    }

    private val jobs = mutableListOf(
        OnlineJob("j1", "Поднятие уровня в одиночку", 2, "AniLibria", isFilm = false, status = JobStatus.RUNNING,
            progress = 0.42f, doneEpisodes = 5, totalEpisodes = 13, error = "", createdAt = 2.0),
        OnlineJob("j2", "Шогун", 1, "HDrezka Studio", isFilm = false, status = JobStatus.FAILED,
            progress = 0.3f, doneEpisodes = 3, totalEpisodes = 10, error = "сайт не отдал серию 4", createdAt = 1.0),
    )

    override suspend fun getOnlineJobs(): List<OnlineJob> {
        delay(latencyMs)
        return jobs.toList()
    }

    override suspend fun retryOnlineJob(id: String) {
        delay(latencyMs / 2)
        val i = jobs.indexOfFirst { it.id == id }
        if (i >= 0) jobs[i] = jobs[i].copy(status = JobStatus.QUEUED, error = "")
    }

    override suspend fun pauseTorrent(hash: String) = setState(hash, TorrentState.PAUSED)

    override suspend fun resumeTorrent(hash: String) = setState(hash, TorrentState.DOWNLOADING)

    private suspend fun setState(hash: String, state: TorrentState) {
        delay(latencyMs / 2)
        val i = torrents.indexOfFirst { it.hash == hash }
        if (i >= 0) torrents[i] = torrents[i].copy(state = state, downloadSpeed = 0)
    }

    override suspend fun search(query: String, language: String): List<SearchResult> {
        delay(latencyMs)
        if (query.isBlank()) return emptyList()
        return listOf(
            SearchResult(438631, TitleType.MOVIE, "Дюна", 2021, "Наследник знаменитого дома Атрейдесов…",
                null, MediaStatus.AVAILABLE),
            SearchResult(693134, TitleType.MOVIE, "Дюна: Часть вторая", 2024, "Пол Атрейдес объединяется…",
                null, MediaStatus.REQUESTED),
            SearchResult(90228, TitleType.SERIES, "Дюна: Пророчество", 2024, "За 10 000 лет до…",
                null, MediaStatus.NONE),
        ).filter { it.name.contains(query, ignoreCase = true) || query.length < 3 }
    }

    override suspend fun request(result: SearchResult) {
        delay(latencyMs)
    }

    override suspend fun getServerStatus(): ServerStatus {
        delay(latencyMs)
        return ServerStatus(
            serverName = "debian",
            diskUsedBytes = 144_778_027_008,
            diskTotalBytes = 310_831_661_056,
            services = listOf(
                ServiceStatus("Jellyfin", true, "12.3.0"),
                ServiceStatus("qBittorrent", true, "5.1"),
                ServiceStatus("Jellyseerr", false, null),
                ServiceStatus("Radarr", true, "5.x"),
            ),
        )
    }

    override suspend fun refreshLibrary() {
        delay(latencyMs)
    }

    override suspend fun getTitleInfo(type: TitleType, tmdbId: Int): TitleInfo {
        delay(latencyMs)
        return TitleInfo(tmdbId, type, "Дюна", "Dune", "Dune", 2021, isAnime = false, tvdbId = null)
    }

    override suspend fun searchReleases(info: TitleInfo): List<Release> {
        delay(latencyMs * 2)
        return listOf(
            Release("g1", 6, "RuTracker.org", "Dune (2021) WEB-DL 2160p HDR Dub RUS Eng", 58_300_000_000, 214, 12),
            Release("g2", 3, "NoNaMe Club", "Dune: Part One (2021) BDRip 1080p MVO", 14_700_000_000, 96, 3),
            Release("g3", 5, "The Pirate Bay", "Dune.2021.1080p.WEBRip.x265", 2_100_000_000, 40, 8),
        )
    }

    override suspend fun isInArr(info: TitleInfo): Boolean = false

    override suspend fun getProfiles(type: TitleType): List<QualityProfile> =
        listOf(QualityProfile(4, "HD-1080p"), QualityProfile(7, "1080p · RU"), QualityProfile(5, "Ultra-HD"))

    override suspend fun grabRelease(info: TitleInfo, release: Release, profileId: Int?) {
        delay(latencyMs * 3)
    }

    override suspend fun searchOnline(info: TitleInfo): List<OnlineResult> {
        delay(latencyMs * 2)
        return listOf(OnlineResult("demo", 0, OnlineSite.REZKA, "Дюна", "2021", null))
    }

    override suspend fun getOnlineDetails(result: OnlineResult, season: Int?): OnlineDetails {
        delay(latencyMs)
        return OnlineDetails(result.title, isFilm = true, seasons = listOf(1), season = 1, totalEpisodes = 1,
            dubs = listOf("Дубляж", "Оригинал (+субтитры)"), qualities = listOf(1080, 720, 480), existing = emptyList())
    }

    override suspend fun queueOnline(
        result: OnlineResult, details: OnlineDetails, dub: String, quality: Int, season: Int, episodes: List<Int>,
    ) {
        delay(latencyMs)
    }

    private fun title(
        id: String, type: TitleType, name: String, original: String, year: Int, rating: Double,
        played: Boolean, percent: Int = if (played) 100 else 0, episodes: Int = 0, unplayed: Int = 0,
    ) = Title(
        id = id, type = type, name = name, originalName = original, year = year,
        overview = "Описание тайтла «$name». На настоящем сервере оно приходит из Jellyfin.",
        rating = rating, played = played, playedPercent = percent,
        episodeCount = episodes, unplayedCount = unplayed, posterUrl = null,
    )
}
