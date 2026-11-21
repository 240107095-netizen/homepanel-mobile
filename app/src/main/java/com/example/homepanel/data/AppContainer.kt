package com.example.homepanel.data

import com.example.homepanel.BuildConfig
import com.example.homepanel.data.network.ApiKeyInterceptor
import com.example.homepanel.data.network.JellyfinApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Ручной DI: одно место, где создаются зависимости приложения.
// ViewModel получают репозиторий отсюда, а не создают его сами.
interface AppContainer {
    val repository: MediaRepository
}

class DefaultAppContainer : AppContainer {

    // Ключей нет (local.properties пуст) — работаем на демо-данных
    private val hasServer = BuildConfig.JELLYFIN_URL.isNotBlank() && BuildConfig.JELLYFIN_KEY.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    // Один HTTP-клиент на все сервисы; каждый перехватчик добавляет ключ только своему сервису
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                ApiKeyInterceptor(
                    BuildConfig.JELLYFIN_URL,
                    "Authorization",
                    "MediaBrowser Token=\"${BuildConfig.JELLYFIN_KEY}\"",
                ),
            )
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    override val repository: MediaRepository by lazy {
        val demo = FakeMediaRepository()
        if (!hasServer) {
            demo
        } else {
            NetworkMediaRepository(
                jellyfin = retrofit(BuildConfig.JELLYFIN_URL).create(JellyfinApi::class.java),
                fallback = demo,
            )
        }
    }
}
