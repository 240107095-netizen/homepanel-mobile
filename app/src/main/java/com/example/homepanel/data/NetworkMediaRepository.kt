package com.example.homepanel.data

import com.example.homepanel.data.model.Episode
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.network.JellyfinApi
import com.example.homepanel.data.network.dto.toEpisode
import com.example.homepanel.data.network.dto.toSeason
import com.example.homepanel.data.network.dto.toTitle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Настоящие данные с сервера. Библиотека — из Jellyfin.
// Остальное пока берётся у fallback (демо), сервисы подключаются по одному.
class NetworkMediaRepository(
    private val jellyfin: JellyfinApi,
    private val jellyfinUrl: String,
    private val fallback: MediaRepository,
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

    override suspend fun getTorrents(): List<Torrent> = fallback.getTorrents()
    override suspend fun pauseTorrent(hash: String) = fallback.pauseTorrent(hash)
    override suspend fun resumeTorrent(hash: String) = fallback.resumeTorrent(hash)
    override suspend fun search(query: String): List<SearchResult> = fallback.search(query)
    override suspend fun request(result: SearchResult) = fallback.request(result)
    override suspend fun getServerStatus(): ServerStatus = fallback.getServerStatus()
}
