package com.example.homepanel.data.network

import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.network.dto.toTorrent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

// Хранит cookie в памяти: после login qBittorrent отдаёт SID, дальше он идёт в каждом запросе
class MemoryCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { this.cookies["${it.domain}:${it.name}"] = it }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.values.filter { it.matches(url) }
}

// Обёртка над QbitApi: сама логинится, если сессии нет или она истекла (403)
class QbitClient(
    private val api: QbitApi,
    private val username: String,
    private val password: String,
) {
    private val loginMutex = Mutex()

    private suspend fun login() = loginMutex.withLock {
        val r = api.login(username, password)
        if (!r.isSuccessful) throw HttpException(r)
        // При неверном пароле qBittorrent отвечает 200 с текстом «Fails.»
        if (r.body()?.trim() == "Fails.") throw HttpException(Response.error<Unit>(401, okhttp3.ResponseBody.create(null, "")))
    }

    // Выполнить запрос; на 403 — залогиниться и повторить один раз
    private suspend fun <T> call(block: suspend () -> Response<T>): Response<T> {
        val first = block()
        if (first.code() != 403) return first
        login()
        return block()
    }

    private fun <T> Response<T>.orThrow(): Response<T> = if (isSuccessful) this else throw HttpException(this)

    suspend fun version(): String = call { api.version() }.orThrow().body().orEmpty()

    suspend fun torrents(): List<Torrent> = call { api.torrents() }.orThrow().body().orEmpty().map { it.toTorrent() }

    // Сначала метод v5, если сервер его не знает (404) — метод v4
    suspend fun pause(hash: String) {
        val r = call { api.stop(hash) }
        if (r.code() == 404) call { api.pause(hash) }.orThrow() else r.orThrow()
    }

    suspend fun resume(hash: String) {
        val r = call { api.start(hash) }
        if (r.code() == 404) call { api.resume(hash) }.orThrow() else r.orThrow()
    }
}
