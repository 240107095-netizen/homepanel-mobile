package com.example.homepanel.data

import com.example.homepanel.data.model.Episode
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.ServiceStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.network.JellyfinApi
import com.example.homepanel.data.network.QbitClient
import com.example.homepanel.data.network.RadarrApi
import com.example.homepanel.data.network.SeerrApi
import com.example.homepanel.data.network.dto.SeerrRequestBody
import com.example.homepanel.data.network.dto.toEpisode
import com.example.homepanel.data.network.dto.toSearchResult
import com.example.homepanel.data.network.dto.toSeason
import com.example.homepanel.data.network.dto.toTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder

// Настоящие данные с сервера:
// библиотека — Jellyfin, загрузки — qBittorrent, поиск и заявки — Jellyseerr, диск — Radarr.
class NetworkMediaRepository(
    private val jellyfin: JellyfinApi,
    private val jellyfinUrl: String,
    private val qbit: QbitClient,
    private val seerr: SeerrApi,
    private val radarr: RadarrApi,
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

    override suspend fun search(query: String): List<SearchResult> {
        // Пробел — %20, а не «+»: иначе Jellyseerr отвечает 400
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return seerr.search(encoded).results.mapNotNull { it.toSearchResult() }
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

    private fun service(name: String, version: String?) = ServiceStatus(name, version != null, version)

    // null — сервис не ответил
    private suspend fun <T> probe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
