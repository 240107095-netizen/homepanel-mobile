package com.example.homepanel.data.network

import com.example.homepanel.data.model.QualityProfile
import com.example.homepanel.data.model.Release
import com.example.homepanel.data.model.ReleaseNotFoundException
import com.example.homepanel.data.model.TitleInfo
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.network.dto.ArrGrabBody
import com.example.homepanel.data.network.dto.ArrRelease
import com.example.homepanel.data.network.dto.toModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Скачивание выбранной раздачи через Radarr/Sonarr (как в десктопном homepanel):
// 1) тайтла нет в сервисе — добавляем (без автопоиска);
// 2) просим сервис найти раздачи сам и берём ту же (по guid или названию+размеру);
// 3) grab — сервис отдаёт её в qBittorrent и потом сам заберёт файл в медиатеку.
class ArrGrabber(
    private val radarr: RadarrLibraryApi,
    private val sonarr: SonarrApi,
) {
    suspend fun profiles(type: TitleType): List<QualityProfile> =
        (if (type == TitleType.MOVIE) radarr.profiles() else sonarr.profiles()).map { it.toModel() }

    // Уже в библиотеке Radarr/Sonarr? Тогда профиль спрашивать не нужно.
    suspend fun isAdded(info: TitleInfo): Boolean = when (info.type) {
        TitleType.MOVIE -> radarr.byTmdb(info.tmdbId).isNotEmpty()
        TitleType.SERIES -> info.tvdbId != null && sonarr.byTvdb(info.tvdbId).isNotEmpty()
    }

    suspend fun grab(info: TitleInfo, release: Release, profileId: Int?) {
        if (info.type == TitleType.MOVIE) grabMovie(info, release, profileId) else grabSeries(info, release, profileId)
    }

    private suspend fun grabMovie(info: TitleInfo, release: Release, profileId: Int?) {
        val movieId = radarr.byTmdb(info.tmdbId).firstOrNull()?.id ?: run {
            val root = radarr.rootFolders().first().path
            val body = radarr.lookup(info.tmdbId).with(
                "qualityProfileId" to JsonPrimitive(profileId ?: radarr.profiles().first().id),
                "rootFolderPath" to JsonPrimitive(root),
                "monitored" to JsonPrimitive(true),
                "minimumAvailability" to JsonPrimitive("released"),
                "addOptions" to buildJsonObject { put("searchForMovie", false) },
            )
            radarr.add(body).id
        }
        val match = radarr.releases(movieId).findSame(release) ?: throw ReleaseNotFoundException()
        radarr.grab(ArrGrabBody(match.guid, match.indexerId))
    }

    private suspend fun grabSeries(info: TitleInfo, release: Release, profileId: Int?) {
        val tvdb = info.tvdbId ?: throw ReleaseNotFoundException()
        val seriesId = sonarr.byTvdb(tvdb).firstOrNull()?.id ?: run {
            val roots = sonarr.rootFolders().map { it.path }
            // Аниме — в свою папку и с абсолютной нумерацией серий
            val root = if (info.isAnime) roots.firstOrNull { it.endsWith("anime") } ?: roots.first()
            else roots.firstOrNull { !it.endsWith("anime") } ?: roots.first()
            val body = sonarr.lookup("tvdb:$tvdb").first().with(
                "qualityProfileId" to JsonPrimitive(profileId ?: sonarr.profiles().first().id),
                "rootFolderPath" to JsonPrimitive(root),
                "seriesType" to JsonPrimitive(if (info.isAnime) "anime" else "standard"),
                "monitored" to JsonPrimitive(true),
                "seasonFolder" to JsonPrimitive(true),
                "addOptions" to buildJsonObject {
                    put("monitor", "all")
                    put("searchForMissingEpisodes", false)
                },
            )
            sonarr.add(body).id
        }
        val season = release.seasons.firstOrNull() ?: 1
        val match = sonarr.releases(seriesId, season).findSame(release) ?: throw ReleaseNotFoundException()
        sonarr.grab(ArrGrabBody(match.guid, match.indexerId))
    }

    private fun List<ArrRelease>.findSame(r: Release): ArrRelease? =
        firstOrNull { it.guid == r.guid } ?: firstOrNull { it.title == r.title && it.size == r.sizeBytes }

    private fun JsonObject.with(vararg fields: Pair<String, kotlinx.serialization.json.JsonElement>) =
        JsonObject(this + fields)
}
