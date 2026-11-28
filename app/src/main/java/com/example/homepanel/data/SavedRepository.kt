package com.example.homepanel.data

import com.example.homepanel.data.local.SavedTitle
import com.example.homepanel.data.local.SavedTitleDao
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.TitleType
import kotlinx.coroutines.flow.Flow

// Сохранённое «на потом»: хранится на телефоне в Room, сервер для этого не нужен
interface SavedRepository {
    fun observeSaved(): Flow<List<SavedTitle>>
    suspend fun save(result: SearchResult)
    suspend fun unsave(tmdbId: Int, type: TitleType)
    suspend fun update(title: SavedTitle)
    suspend fun delete(title: SavedTitle)
    suspend fun restore(title: SavedTitle)   // «Отменить» после удаления — та же строка с тем же id
}

class RoomSavedRepository(private val dao: SavedTitleDao) : SavedRepository {

    override fun observeSaved(): Flow<List<SavedTitle>> = dao.observeAll()

    override suspend fun save(result: SearchResult) {
        dao.insert(
            SavedTitle(
                tmdbId = result.tmdbId,
                type = result.type,
                name = result.name,
                year = result.year,
                posterUrl = result.posterUrl,
            ),
        )
    }

    override suspend fun unsave(tmdbId: Int, type: TitleType) = dao.deleteByTmdb(tmdbId, type)

    override suspend fun update(title: SavedTitle) = dao.update(title)

    override suspend fun delete(title: SavedTitle) = dao.delete(title)

    override suspend fun restore(title: SavedTitle) {
        dao.insert(title)
    }
}
