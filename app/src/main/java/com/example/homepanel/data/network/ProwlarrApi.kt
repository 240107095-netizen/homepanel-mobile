package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.ProwlarrRelease
import retrofit2.http.GET
import retrofit2.http.Query

// Prowlarr ищет раздачи сразу по всем трекерам. Только поиск: скачивание идёт
// через Radarr/Sonarr, иначе они не заберут файл в медиатеку.
interface ProwlarrApi {

    // categories: 2000 — кино, 5000 — сериалы (включая аниме)
    @GET("api/v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("categories") categories: List<Int>,
        @Query("type") type: String = "search",
        @Query("limit") limit: Int = 100,
    ): List<ProwlarrRelease>
}
