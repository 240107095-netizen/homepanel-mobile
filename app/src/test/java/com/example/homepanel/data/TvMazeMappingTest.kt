package com.example.homepanel.data

import com.example.homepanel.data.model.ShowStatus
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.data.network.dto.TvMazeShow
import com.example.homepanel.data.network.dto.toSchedule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun schedule() = json.decodeFromString<TvMazeShow>(response).toSchedule(title, today = "2026-11-28")

    @Test
    fun countsOnlyAiredRegularEpisodes() {
        val s = schedule()
        // 4 обычные серии вышли; спецвыпуск и серия из будущего не считаются
        assertEquals(4, s.airedEpisodes)
        assertEquals(2, s.missing)
        assertEquals(2, s.lastAired?.season)
        assertEquals(2, s.lastAired?.number)
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
    fun nothingMissingWhenServerHasMore() {
        val s = json.decodeFromString<TvMazeShow>(response)
            .toSchedule(title.copy(episodeCount = 10), today = "2026-11-28")
        assertEquals(0, s.missing)
    }

    @Test
    fun showWithoutEmbedsStillParses() {
        val s = json.decodeFromString<TvMazeShow>("""{"id": 1, "name": "X", "status": "Ended"}""")
            .toSchedule(title, today = "2026-11-28")
        assertEquals(ShowStatus.ENDED, s.status)
        assertEquals(0, s.airedEpisodes)
        assertNull(s.next)
    }
}
