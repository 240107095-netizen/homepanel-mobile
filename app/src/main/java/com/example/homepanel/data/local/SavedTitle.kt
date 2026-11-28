package com.example.homepanel.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homepanel.data.model.TitleType

// Тайтл, который пользователь сохранил «на потом» из поиска. Одна строка таблицы saved_titles.
// Один и тот же фильм дважды не сохранить: уникальный индекс по (tmdb_id, type).
@Entity(
    tableName = "saved_titles",
    indices = [Index(value = ["tmdb_id", "type"], unique = true)],
)
data class SavedTitle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    val type: TitleType,                                    // Room хранит enum строкой: MOVIE / SERIES
    val name: String,
    val year: Int?,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    val note: String = "",                                  // своя заметка: «посмотреть с братом»
    val watched: Boolean = false,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)
