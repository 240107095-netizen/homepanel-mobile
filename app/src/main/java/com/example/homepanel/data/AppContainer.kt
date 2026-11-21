package com.example.homepanel.data

import com.example.homepanel.BuildConfig
import com.example.homepanel.data.network.ApiKeyInterceptor
import com.example.homepanel.data.network.ArrGrabber
import com.example.homepanel.data.network.JellyfinApi
import com.example.homepanel.data.network.MediaDlApi
import com.example.homepanel.data.network.MemoryCookieJar
import com.example.homepanel.data.network.QbitApi
import com.example.homepanel.data.network.ProwlarrApi
import com.example.homepanel.data.network.QbitClient
import com.example.homepanel.data.network.RadarrLibraryApi
import com.example.homepanel.data.network.SonarrApi
import com.example.homepanel.data.network.RadarrApi
import com.example.homepanel.data.network.SeerrApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Ручной DI: одно место, где создаются зависимости приложения.
// ViewModel получают репозиторий отсюда, а не создают его сами.
interface AppContainer {
    val repository: MediaRepository
    val httpClient: OkHttpClient
}

class DefaultAppContainer : AppContainer {

    // Ключей нет (local.properties пуст) — работаем на демо-данных
    private val hasServer = BuildConfig.JELLYFIN_URL.isNotBlank() && BuildConfig.JELLYFIN_KEY.isNotBlank()

    // Незнакомые поля пропускаем, null-поля не отправляем (Jellyseerr не любит "seasons": null)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Один HTTP-клиент на все сервисы; каждый перехватчик добавляет ключ только своему сервису
    override val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                ApiKeyInterceptor(
                    BuildConfig.JELLYFIN_URL,
                    "Authorization",
                    "MediaBrowser Token=\"${BuildConfig.JELLYFIN_KEY}\"",
                ),
            )
            .addInterceptor(ApiKeyInterceptor(BuildConfig.SEERR_URL, "X-Api-Key", BuildConfig.SEERR_KEY))
            .addInterceptor(ApiKeyInterceptor(BuildConfig.RADARR_URL, "X-Api-Key", BuildConfig.RADARR_KEY))
            .addInterceptor(ApiKeyInterceptor(BuildConfig.SONARR_URL, "X-Api-Key", BuildConfig.SONARR_KEY))
            .addInterceptor(ApiKeyInterceptor(BuildConfig.PROWLARR_URL, "X-Api-Key", BuildConfig.PROWLARR_KEY))
            .cookieJar(MemoryCookieJar())  // сессия qBittorrent
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Живой поиск по трекерам и онлайн-сайтам идёт до минуты-двух — свой длинный таймаут
    private val slowClient: OkHttpClient by lazy {
        httpClient.newBuilder().readTimeout(150, TimeUnit.SECONDS).build()
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient = httpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())  // ответы-строки («Ok.», версия)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    override val repository: MediaRepository by lazy {
        if (!hasServer) {
            FakeMediaRepository()
        } else {
            NetworkMediaRepository(
                jellyfin = retrofit(BuildConfig.JELLYFIN_URL).create(JellyfinApi::class.java),
                jellyfinUrl = BuildConfig.JELLYFIN_URL,
                qbit = QbitClient(
                    retrofit(BuildConfig.QBIT_URL).create(QbitApi::class.java),
                    BuildConfig.QBIT_USER,
                    BuildConfig.QBIT_PASSWORD,
                ),
                seerr = retrofit(BuildConfig.SEERR_URL).create(SeerrApi::class.java),
                radarr = retrofit(BuildConfig.RADARR_URL).create(RadarrApi::class.java),
                prowlarr = retrofit(BuildConfig.PROWLARR_URL, slowClient).create(ProwlarrApi::class.java),
                arr = ArrGrabber(
                    retrofit(BuildConfig.RADARR_URL, slowClient).create(RadarrLibraryApi::class.java),
                    retrofit(BuildConfig.SONARR_URL, slowClient).create(SonarrApi::class.java),
                ),
                mediaDl = retrofit(BuildConfig.MEDIADL_URL, slowClient).create(MediaDlApi::class.java),
            )
        }
    }
}
