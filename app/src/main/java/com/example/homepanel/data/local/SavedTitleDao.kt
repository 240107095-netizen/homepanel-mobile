package com.example.homepanel.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.homepanel.data.model.TitleType
import kotlinx.coroutines.flow.Flow

// Операции с таблицей saved_titles. Реализацию пишет Room (KSP) при сборке.
@Dao
interface SavedTitleDao {

    // Read: Flow сам присылает новый список после каждого изменения таблицы
    @Query("SELECT * FROM saved_titles ORDER BY added_at DESC")
    fun observeAll(): Flow<List<SavedTitle>>

    @Query("SELECT * FROM saved_titles WHERE id = :id")
    suspend fun get(id: Long): SavedTitle?

    // Create: уже сохранён — ничего не делаем (вернёт -1)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(title: SavedTitle): Long

    // Update: заметка, «просмотрено»
    @Update
    suspend fun update(title: SavedTitle)

    // Delete
    @Delete
    suspend fun delete(title: SavedTitle)

    // Убрать по TMDB id — для звёздочки в поиске, где id строки базы неизвестен
    @Query("DELETE FROM saved_titles WHERE tmdb_id = :tmdbId AND type = :type")
    suspend fun deleteByTmdb(tmdbId: Int, type: TitleType)
}
