package com.example.homepanel.data

import com.example.homepanel.data.model.ShowSchedule
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.network.TvMazeApi
import com.example.homepanel.data.network.dto.TvMazeShow
import com.example.homepanel.data.network.dto.toSchedule
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TVmaze не ответил (нет интернета или сервис лежит) — отдельный тип, чтобы экран
// написал «TVmaze недоступен», а не «нет связи с домашним сервером»
class TvMazeUnavailableException(cause: Throwable) : IOException(cause)

// Новые серии: какие сериалы из библиотеки уже вышли дальше, чем лежит на сервере
interface ScheduleRepository {
    suspend fun getSchedules(): List<ShowSchedule>
}

class TvMazeScheduleRepository(
    private val library: MediaRepository,   // откуда берём сериалы (Jellyfin или демо)
    private val tvMaze: TvMazeApi,
) : ScheduleRepository {

    override suspend fun getSchedules(): List<ShowSchedule> = coroutineScope {
        val series = library.getLibrary().filter { it.type == TitleType.SERIES }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        // Все сериалы спрашиваем параллельно, но не больше 4 запросов сразу — у TVmaze лимит
        val gate = Semaphore(4)
        series.map { title ->
            async {
                val show = gate.withPermit { find(title) } ?: return@async null
                // Сколько серий каждого сезона уже лежит на сервере
                val seasons = library.getSeasons(title.id).associate { it.number to it.episodeCount }
                show.toSchedule(title, seasons, today)
            }
        }.awaitAll().filterNotNull()
    }

    // Ищем по оригинальному названию (TVmaze англоязычный), не нашли — по названию из библиотеки.
    // null — TVmaze такого сериала не знает.
    private suspend fun find(title: Title): TvMazeShow? {
        val names = listOf(title.originalName, title.name).filter { it.isNotBlank() }.distinct()
        for (name in names) {
            request(name)?.let { return it }
        }
        return null
    }

    private suspend fun request(name: String, retry: Boolean = true): TvMazeShow? = try {
        tvMaze.findShow(name)
    } catch (e: HttpException) {
        when {
            e.code() == 404 -> null
            // Упёрлись в лимит — ждём и пробуем ещё раз
            e.code() == 429 && retry -> {
                delay(2_000)
                request(name, retry = false)
            }
            else -> throw TvMazeUnavailableException(e)
        }
    } catch (e: IOException) {
        throw TvMazeUnavailableException(e)
    }
}
