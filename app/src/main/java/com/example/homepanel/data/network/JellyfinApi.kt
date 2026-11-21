package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.JfItems
import com.example.homepanel.data.network.dto.JfSystemInfo
import com.example.homepanel.data.network.dto.JfUser
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// REST API Jellyfin. Ключ добавляет ApiKeyInterceptor
// (заголовок Authorization: MediaBrowser Token="...").
interface JellyfinApi {

    @GET("Users")
    suspend fun users(): List<JfUser>

    // Вся библиотека: фильмы и сериалы со статусом просмотра. ids — один конкретный тайтл.
    @GET("Users/{userId}/Items")
    suspend fun items(
        @Path("userId") userId: String,
        @Query("ids") ids: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") types: String = "Movie,Series",
        @Query("EnableUserData") userData: Boolean = true,
        @Query("Fields") fields: String = "Overview,OriginalTitle,RecursiveItemCount",
        @Query("SortBy") sortBy: String = "SortName",
    ): JfItems

    @GET("Shows/{seriesId}/Seasons")
    suspend fun seasons(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "ChildCount",
        @Query("EnableUserData") userData: Boolean = true,
    ): JfItems

    @GET("Shows/{seriesId}/Episodes")
    suspend fun episodes(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("seasonId") seasonId: String,
        @Query("EnableUserData") userData: Boolean = true,
    ): JfItems

    @GET("System/Info")
    suspend fun systemInfo(): JfSystemInfo

    // Пересканировать папки: новые файлы появятся в библиотеке через минуту-другую
    @POST("Library/Refresh")
    suspend fun refreshLibrary()
}
