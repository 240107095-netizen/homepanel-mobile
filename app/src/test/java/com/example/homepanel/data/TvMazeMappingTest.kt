package com.example.homepanel.data

import com.example.homepanel.data.model.ShowStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.network.dto.TvMazeShow
import com.example.homepanel.data.network.dto.toSchedule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// JSON → data class → модель экрана, без сети: кусок настоящего ответа TVmaze
class TvMazeMappingTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private val response = """
        {
          "id": 48945, "name": "Cyberpunk: Edgerunners", "premiered": "2022-09-13",
          "status": "To Be Determined", "network": null, "webChannel": {"id": 1, "name": "Netflix"},
          "image": {"medium": "https://static.tvmaze.com/m.jpg", "original": "https://static.tvmaze.com/o.jpg"},
          "_embedded": {
            "nextepisode": {"season": 2, "number": 3, "name": "Next", "airdate": "2026-12-04", "type": "regular"},
            "episodes": [
              {"season": 1, "number": 1, "name": "Let You Down", "airdate": "2022-09-13", "type": "regular"},
              {"season": 1, "number": 2, "name": "Like A Boy", "airdate": "2022-09-13", "type": "regular"},
              {"season": 1, "number": null, "name": "Special", "airdate": "2022-10-01", "type": "significant_special"},
              {"season": 2, "number": 1, "name": "New", "airdate": "2026-10-20", "type": "regular"},
              {"season": 2, "number": 2, "name": "Newer", "airdate": "2026-10-20", "type": "regular"},
              {"season": 2, "number": 3, "name": "Next", "airdate": "2026-12-04", "type": "regular"}
            ]
          }
        }
    """.trimIndent()

    private val title = Title(
        id = "s1", type = TitleType.SERIES, name = "Киберпанк: Бегущие по краю", originalName = "Cyberpunk: Edgerunners",
        year = 2022, overview = "", rating = null, played = false, playedPercent = 0,
        episodeCount = 2, unplayedCount = 0, posterUrl = null,
    )

    // На сервере только 1-й сезон: 2 серии
    private fun schedule(server: Map<Int, Int> = mapOf(1 to 2)) =
        json.decodeFromString<TvMazeShow>(response).toSchedule(title, server, today = "2026-11-28")

    @Test
    fun countsOnlyAiredRegularEpisodes() {
        val s = schedule()
        // В 1-м сезоне 2 обычные серии, спецвыпуск не считается; обе на сервере
        assertEquals(2, s.airedEpisodes)
        assertEquals(0, s.missing)
        assertEquals(2, s.lastAired?.season)
        assertEquals(2, s.lastAired?.number)
    }

    @Test
    fun laterSeasonIsReportedSeparately() {
        val s = schedule()
        // Из 2-го сезона вышли 2 серии, третья — в будущем
        assertEquals(listOf(2), s.newSeasons)
        assertEquals(2, s.newSeasonEpisodes)
        assertTrue(s.isBehind)
    }

    @Test
    fun missingCountedInsideSeasonsOnServer() {
        val s = schedule(server = mapOf(1 to 1))
        assertEquals(1, s.missing)
        assertEquals(1, s.onServer)
    }

    @Test
    fun earlierSeasonsNotOnServerAreIgnored() {
        // Скачан только 2-й сезон целиком (вышедшие 2 серии) — 1-й не качали, это не «отставание»
        val s = schedule(server = mapOf(2 to 2))
        assertEquals(0, s.missing)
        assertTrue(s.newSeasons.isEmpty())
        assertFalse(s.isBehind)
    }

    @Test
    fun mapsStatusNetworkAndNextEpisode() {
        val s = schedule()
        assertEquals(ShowStatus.TBD, s.status)
        assertEquals("Netflix", s.network)             // нет телеканала — берём стриминг
        assertEquals("2026-12-04", s.next?.airdate)
        assertEquals("https://static.tvmaze.com/m.jpg", s.posterUrl)
        assertEquals("Киберпанк: Бегущие по краю", s.name)  // название — как в библиотеке
    }

    @Test
    fun showWithoutEmbedsStillParses() {
        val s = json.decodeFromString<TvMazeShow>("""{"id": 1, "name": "X", "status": "Ended"}""")
            .toSchedule(title, mapOf(1 to 2), today = "2026-11-28")
        assertEquals(ShowStatus.ENDED, s.status)
        assertEquals(0, s.airedEpisodes)
        assertNull(s.next)
    }
}
