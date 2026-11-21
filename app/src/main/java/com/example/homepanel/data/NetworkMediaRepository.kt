package com.example.homepanel.data

import com.example.homepanel.data.model.Episode
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
import com.example.homepanel.data.network.ArrGrabber
import com.example.homepanel.data.network.JellyfinApi
import com.example.homepanel.data.network.MediaDlApi
import com.example.homepanel.data.network.ProwlarrApi
import com.example.homepanel.data.network.QbitClient
import com.example.homepanel.data.network.RadarrApi
import com.example.homepanel.data.network.SeerrApi
import com.example.homepanel.data.network.dto.MdlJobBody
import com.example.homepanel.data.network.dto.SeerrRequestBody
import com.example.homepanel.data.network.dto.detailsToTitleInfo
import com.example.homepanel.data.network.dto.toModel
import com.example.homepanel.data.network.dto.toRelease
import com.example.homepanel.data.network.dto.toEpisode
import com.example.homepanel.data.network.dto.toSearchResult
import com.example.homepanel.data.network.dto.toSeason
import com.example.homepanel.data.network.dto.toTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder

// Настоящие данные с сервера:
// библиотека — Jellyfin, загрузки — qBittorrent, поиск и заявки — Jellyseerr, диск — Radarr,
// раздачи — Prowlarr (скачивание через Radarr/Sonarr), онлайн-сайты — MediaDL.
class NetworkMediaRepository(
    private val jellyfin: JellyfinApi,
    private val jellyfinUrl: String,
    private val qbit: QbitClient,
    private val seerr: SeerrApi,
    private val radarr: RadarrApi,
    private val prowlarr: ProwlarrApi,
    private val arr: ArrGrabber,
    private val mediaDl: MediaDlApi,
) : MediaRepository {

    // Учётка в Jellyfin одна (семья смотрит под ней) — её id запоминаем после первого запроса
    private val userMutex = Mutex()
    private var userId: String? = null

    private suspend fun userId(): String = userMutex.withLock {
        userId ?: jellyfin.users().first().id.also { userId = it }
    }

    override suspend fun getLibrary(): List<Title> =
        jellyfin.items(userId()).items.map { it.toTitle(jellyfinUrl) }

    override suspend fun getTitle(id: String): Title =
        jellyfin.items(userId(), ids = id).items.first().toTitle(jellyfinUrl)

    override suspend fun getSeasons(seriesId: String): List<Season> =
        jellyfin.seasons(seriesId, userId()).items.map { it.toSeason(seriesId) }

    override suspend fun getEpisodes(seriesId: String, seasonId: String): List<Episode> =
        jellyfin.episodes(seriesId, userId(), seasonId).items.map { it.toEpisode() }

    override suspend fun getTorrents(): List<Torrent> = qbit.torrents()
    override suspend fun pauseTorrent(hash: String) = qbit.pause(hash)
    override suspend fun resumeTorrent(hash: String) = qbit.resume(hash)

    override suspend fun search(query: String, language: String): List<SearchResult> {
        // Пробел — %20, а не «+»: иначе Jellyseerr отвечает 400
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return seerr.search(encoded, language = language).results.mapNotNull { it.toSearchResult() }
    }

    override suspend fun request(result: SearchResult) {
        val isSeries = result.type == TitleType.SERIES
        seerr.request(
            SeerrRequestBody(
                mediaType = if (isSeries) "tv" else "movie",
                mediaId = result.tmdbId,
                seasons = if (isSeries) "all" else null,
            ),
        )
    }

    // Опрашиваем все сервисы параллельно; упавший сервис не ломает весь экран
    override suspend fun getServerStatus(): ServerStatus = coroutineScope {
        val jf = async { probe { jellyfin.systemInfo() } }
        val qb = async { probe { qbit.version() } }
        val js = async { probe { seerr.status().version } }
        val ra = async { probe { radarr.status().version } }
        val disks = async { probe { radarr.diskSpace() } }

        val info = jf.await()
        val disk = disks.await()?.let { list -> list.firstOrNull { it.path == "/data" } ?: list.maxByOrNull { it.totalSpace } }
        if (info == null && disk == null) throw IllegalStateException("server is down")

        ServerStatus(
            serverName = info?.serverName.orEmpty(),
            diskUsedBytes = disk?.let { it.totalSpace - it.freeSpace } ?: 0,
            diskTotalBytes = disk?.totalSpace ?: 0,
            services = listOf(
                ServiceStatus("Jellyfin", info != null, info?.version),
                service("qBittorrent", qb.await()),
                service("Jellyseerr", js.await()),
                service("Radarr", ra.await()),
            ),
        )
    }

    override suspend fun refreshLibrary() = jellyfin.refreshLibrary()

    // Английское название — для трекеров, русское — для онлайн-сайтов (они русскоязычные)
    override suspend fun getTitleInfo(type: TitleType, tmdbId: Int): TitleInfo = coroutineScope {
        val en = async { if (type == TitleType.MOVIE) seerr.movie(tmdbId, "en") else seerr.tv(tmdbId, "en") }
        val ru = async { if (type == TitleType.MOVIE) seerr.movie(tmdbId, "ru") else seerr.tv(tmdbId, "ru") }
        detailsToTitleInfo(en.await(), ru.await(), type)
    }

    override suspend fun searchReleases(info: TitleInfo): List<Release> {
        val categories = listOf(if (info.type == TitleType.MOVIE) 2000 else 5000)
        var found = prowlarr.search(info.trackerQuery, categories)
        // Без подзаголовка раздачи находятся чаще
        if (found.isEmpty()) info.shortQuery?.let { found = prowlarr.search(it, categories) }
        return found.map { it.toRelease() }
            .distinctBy { it.guid }
            .filter { info.isAnime || it.indexer !in ANIME_ONLY_INDEXERS }
            .sortedWith(compareByDescending<Release> { it.seeders }.thenByDescending { it.sizeBytes })
    }

    override suspend fun isInArr(info: TitleInfo): Boolean = arr.isAdded(info)

    override suspend fun getProfiles(type: TitleType): List<QualityProfile> = arr.profiles(type)

    override suspend fun grabRelease(info: TitleInfo, release: Release, profileId: Int?) =
        arr.grab(info, release, profileId)

    // Аниме ищем на AnimeGo и HDRezka, остальное — на HDRezka. Сайт упал — показываем остальные.
    override suspend fun searchOnline(info: TitleInfo): List<OnlineResult> = coroutineScope {
        val sites = if (info.isAnime) listOf(OnlineSite.ANIMEGO, OnlineSite.REZKA) else listOf(OnlineSite.REZKA)
        val query = info.name.ifBlank { info.originalName }
        val answers = sites.map { site ->
            async { probe { site to mediaDl.search(query, site.category, site.source) } }
        }.awaitAll()
        if (answers.all { it == null }) throw IllegalStateException("online sources are down")
        answers.filterNotNull().flatMap { (site, page) -> page.results.map { it.toModel(page.searchId, site) } }
    }

    override suspend fun getOnlineDetails(result: OnlineResult, season: Int?): OnlineDetails {
        val d = mediaDl.details(result.searchId, result.idx, season)
        if (d.error != null) throw IllegalStateException(d.error)
        return d.toModel()
    }

    override suspend fun queueOnline(
        result: OnlineResult, details: OnlineDetails, dub: String, quality: Int, season: Int, episodes: List<Int>,
    ) {
        mediaDl.createJob(
            MdlJobBody(
                searchId = result.searchId,
                idx = result.idx,
                mediaType = result.site.category,
                dub = dub,
                quality = quality.toString(),
                season = season,
                episodes = if (details.isFilm) listOf(0) else episodes,
                showTitle = details.title,
            ),
        )
    }

    private fun service(name: String, version: String?) = ServiceStatus(name, version != null, version)

    // null — сервис не ответил
    private companion object {
        // Трекеры только с аниме: для обычного кино там один шум
        val ANIME_ONLY_INDEXERS = setOf("Nyaa.si", "Anime Tosho")
    }

    private suspend fun <T> probe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
