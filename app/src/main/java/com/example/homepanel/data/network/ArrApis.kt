package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.ArrGrabBody
import com.example.homepanel.data.network.dto.ArrItem
import com.example.homepanel.data.network.dto.ArrProfile
import com.example.homepanel.data.network.dto.ArrRelease
import com.example.homepanel.data.network.dto.ArrRootFolder
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Radarr: фильмы. Тело для добавления — то, что вернул lookup, плюс наши поля (JsonObject как есть).
interface RadarrLibraryApi {
    @GET("api/v3/movie")
    suspend fun byTmdb(@Query("tmdbId") tmdbId: Int): List<ArrItem>

    @GET("api/v3/movie/lookup/tmdb")
    suspend fun lookup(@Query("tmdbId") tmdbId: Int): JsonObject

    @POST("api/v3/movie")
    suspend fun add(@Body body: JsonObject): ArrItem

    @GET("api/v3/qualityprofile")
    suspend fun profiles(): List<ArrProfile>

    @GET("api/v3/rootfolder")
    suspend fun rootFolders(): List<ArrRootFolder>

    // Живой поиск по трекерам — до минуты
    @GET("api/v3/release")
    suspend fun releases(@Query("movieId") movieId: Int): List<ArrRelease>

    @POST("api/v3/release")
    suspend fun grab(@Body body: ArrGrabBody)
}

// Sonarr: сериалы (знает их по TVDB id)
interface SonarrApi {
    @GET("api/v3/series")
    suspend fun byTvdb(@Query("tvdbId") tvdbId: Int): List<ArrItem>

    @GET("api/v3/series/lookup")
    suspend fun lookup(@Query("term") term: String): List<JsonObject>

    @POST("api/v3/series")
    suspend fun add(@Body body: JsonObject): ArrItem

    @GET("api/v3/qualityprofile")
    suspend fun profiles(): List<ArrProfile>

    @GET("api/v3/rootfolder")
    suspend fun rootFolders(): List<ArrRootFolder>

    @GET("api/v3/release")
    suspend fun releases(@Query("seriesId") seriesId: Int, @Query("seasonNumber") season: Int): List<ArrRelease>

    @POST("api/v3/release")
    suspend fun grab(@Body body: ArrGrabBody)
}
