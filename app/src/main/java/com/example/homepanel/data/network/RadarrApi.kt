package com.example.homepanel.data.network

import com.example.homepanel.data.network.dto.RadarrDisk
import com.example.homepanel.data.network.dto.RadarrStatus
import retrofit2.http.GET

// Radarr знает про диски сервера — берём у него место на /data
interface RadarrApi {

    @GET("api/v3/diskspace")
    suspend fun diskSpace(): List<RadarrDisk>

    @GET("api/v3/system/status")
    suspend fun status(): RadarrStatus
}
