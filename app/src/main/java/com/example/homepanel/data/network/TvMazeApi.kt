package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.TvMazeShow
import retrofit2.http.GET
import retrofit2.http.Query

// TVmaze — публичный REST API о сериалах (https://www.tvmaze.com/api).
// Без ключа и регистрации, лимит ~20 запросов за 10 секунд.
interface TvMazeApi {

    // Лучшее совпадение по названию. embed[] добавляет в ответ следующую серию и список всех серий,
    // так что на один сериал нужен один запрос. Не нашёл — 404.
    @GET("singlesearch/shows")
    suspend fun findShow(
        @Query("q") name: String,
        @Query("embed[]") embed: List<String> = listOf("nextepisode", "episodes"),
    ): TvMazeShow

    companion object {
        const val BASE_URL = "https://api.tvmaze.com/"
    }
}
