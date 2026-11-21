package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.MdlDetails
import com.example.homepanel.data.network.dto.MdlJob
import com.example.homepanel.data.network.dto.MdlJobBody
import com.example.homepanel.data.network.dto.MdlSearch
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// MediaDL — сервис на сервере, который качает с онлайн-сайтов (HDRezka, AnimeGo)
// прямо в медиатеку. Без ключа: доступен только из домашней сети.
interface MediaDlApi {

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("category") category: String,
        @Query("source") source: String,
    ): MdlSearch

    @GET("api/details")
    suspend fun details(
        @Query("search_id") searchId: String,
        @Query("idx") idx: Int,
        @Query("season") season: Int? = null,
    ): MdlDetails

    // Поставить задачу в очередь (сервер заранее собирает список серий — бывает долго)
    @POST("api/jobs")
    suspend fun createJob(@Body body: MdlJobBody): MdlJob
}
