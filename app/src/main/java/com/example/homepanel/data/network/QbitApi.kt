package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.QbitTorrent
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Web API qBittorrent. Авторизация — cookie SID после login (её хранит CookieJar).
interface QbitApi {

    @FormUrlEncoded
    @POST("api/v2/auth/login")
    suspend fun login(@Field("username") username: String, @Field("password") password: String): Response<String>

    @GET("api/v2/app/version")
    suspend fun version(): Response<String>

    @GET("api/v2/torrents/info")
    suspend fun torrents(
        @Query("sort") sort: String = "added_on",
        @Query("reverse") reverse: Boolean = true,
    ): Response<List<QbitTorrent>>

    // qBittorrent 5 (API v5): stop/start
    @FormUrlEncoded
    @POST("api/v2/torrents/stop")
    suspend fun stop(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/start")
    suspend fun start(@Field("hashes") hashes: String): Response<Unit>

    // qBittorrent 4: те же действия назывались pause/resume
    @FormUrlEncoded
    @POST("api/v2/torrents/pause")
    suspend fun pause(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/resume")
    suspend fun resume(@Field("hashes") hashes: String): Response<Unit>
}
