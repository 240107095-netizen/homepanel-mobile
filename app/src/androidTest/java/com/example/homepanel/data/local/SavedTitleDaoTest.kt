package com.example.homepanel.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.homepanel.data.model.TitleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// CRUD через настоящий Room, но база в памяти: тест не трогает данные приложения
@RunWith(AndroidJUnit4::class)
class SavedTitleDaoTest {

    private lateinit var db: HomepanelDatabase
    private lateinit var dao: SavedTitleDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, HomepanelDatabase::class.java).build()
        dao = db.savedTitleDao()
    }

    @After
    fun closeDb() = db.close()

    private fun dune() = SavedTitle(tmdbId = 438631, type = TitleType.MOVIE, name = "Дюна", year = 2021, posterUrl = null)

    @Test
    fun insertAndRead() = runBlocking {
        val id = dao.insert(dune())
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Дюна", all.first().name)
        assertEquals(TitleType.MOVIE, dao.get(id)?.type)
    }

    @Test
    fun sameTitleIsSavedOnce() = runBlocking {
        dao.insert(dune())
        val second = dao.insert(dune())
        assertEquals(-1L, second)                       // уникальный индекс (tmdb_id, type)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun update() = runBlocking {
        val id = dao.insert(dune())
        dao.update(dao.get(id)!!.copy(note = "с братом", watched = true))
        val saved = dao.get(id)!!
        assertEquals("с братом", saved.note)
        assertTrue(saved.watched)
    }

    @Test
    fun deleteAndRestore() = runBlocking {
        val id = dao.insert(dune())
        val row = dao.get(id)!!
        dao.delete(row)
        assertTrue(dao.observeAll().first().isEmpty())
        dao.insert(row)                                 // «Отменить» — та же строка с тем же id
        assertEquals(id, dao.observeAll().first().single().id)
    }

    @Test
    fun deleteByTmdbKeepsOtherType() = runBlocking {
        dao.insert(dune())
        dao.insert(dune().copy(type = TitleType.SERIES, name = "Дюна: сериал"))
        dao.deleteByTmdb(438631, TitleType.MOVIE)
        assertEquals(TitleType.SERIES, dao.observeAll().first().single().type)
    }
}
