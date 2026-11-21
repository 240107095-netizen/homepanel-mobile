package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.SeerrDetails
import com.example.homepanel.data.network.dto.SeerrRequestBody
import com.example.homepanel.data.network.dto.SeerrSearchPage
import com.example.homepanel.data.network.dto.SeerrStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Jellyseerr: поиск по TMDB и заявки на скачивание (дальше их выполняют Radarr/Sonarr).
// Ключ — заголовок X-Api-Key (ApiKeyInterceptor).
interface SeerrApi {

    // Jellyseerr строго проверяет кодирование query, поэтому кодируем сами (encoded = true)
    @GET("api/v1/search")
    suspend fun search(
        @Query("query", encoded = true) query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en",
    ): SeerrSearchPage

    @POST("api/v1/request")
    suspend fun request(@Body body: SeerrRequestBody)

    // Подробности: английское название (для трекеров), жанры, язык, TVDB id
    @GET("api/v1/movie/{id}")
    suspend fun movie(@Path("id") tmdbId: Int, @Query("language") language: String = "en"): SeerrDetails

    @GET("api/v1/tv/{id}")
    suspend fun tv(@Path("id") tmdbId: Int, @Query("language") language: String = "en"): SeerrDetails

    @GET("api/v1/status")
    suspend fun status(): SeerrStatus
}
