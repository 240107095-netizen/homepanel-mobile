package com.example.homepanel.data

import com.example.homepanel.data.model.Episode
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.Torrent

// Всё, что нужно экранам от сервера. ViewModel знает только этот интерфейс,
// поэтому фейковые данные и настоящая сеть подменяются без правок UI.
interface MediaRepository {
    suspend fun getLibrary(): List<Title>
    suspend fun getTitle(id: String): Title
    suspend fun getSeasons(seriesId: String): List<Season>
    suspend fun getEpisodes(seriesId: String, seasonId: String): List<Episode>

    suspend fun getTorrents(): List<Torrent>
    suspend fun pauseTorrent(hash: String)
    suspend fun resumeTorrent(hash: String)

    // language — язык интерфейса (en/ru/kk): названия и описания придут на нём
    suspend fun search(query: String, language: String): List<SearchResult>
    suspend fun request(result: SearchResult)

    suspend fun getServerStatus(): ServerStatus
}
