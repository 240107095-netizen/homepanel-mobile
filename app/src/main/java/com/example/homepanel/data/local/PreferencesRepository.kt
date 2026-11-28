package com.example.homepanel.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Как показывать сохранённое: порядок и прятать ли просмотренное
enum class SavedSort { NEWEST, NAME, YEAR }

data class SavedPrefs(
    val sort: SavedSort = SavedSort.NEWEST,
    val hideWatched: Boolean = false,
)

// Настройки пользователя (DataStore — замена SharedPreferences на корутинах и Flow)
interface PreferencesRepository {
    val savedPrefs: Flow<SavedPrefs>
    suspend fun setSavedSort(sort: SavedSort)
    suspend fun setHideWatched(hide: Boolean)
}

// Один файл settings.preferences_pb на всё приложение
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStorePreferencesRepository(context: Context) : PreferencesRepository {

    private val store = context.applicationContext.dataStore

    override val savedPrefs: Flow<SavedPrefs> = store.data.map { prefs ->
        SavedPrefs(
            // Неизвестное значение (например, после переименования) — порядок по умолчанию
            sort = prefs[SORT]?.let { name -> SavedSort.entries.firstOrNull { it.name == name } } ?: SavedSort.NEWEST,
            hideWatched = prefs[HIDE_WATCHED] ?: false,
        )
    }

    override suspend fun setSavedSort(sort: SavedSort) {
        store.edit { it[SORT] = sort.name }
    }

    override suspend fun setHideWatched(hide: Boolean) {
        store.edit { it[HIDE_WATCHED] = hide }
    }

    private companion object {
        val SORT = stringPreferencesKey("saved_sort")
        val HIDE_WATCHED = booleanPreferencesKey("saved_hide_watched")
    }
}
