package com.example.homepanel.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.SavedRepository
import com.example.homepanel.data.local.PreferencesRepository
import com.example.homepanel.data.local.SavedPrefs
import com.example.homepanel.data.local.SavedSort
import com.example.homepanel.data.local.SavedTitle
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val titles: UiState<List<SavedTitle>> = UiState.Loading,   // уже отсортированные и отфильтрованные
    val total: Int = 0,                                         // сколько всего в базе
    val prefs: SavedPrefs = SavedPrefs(),
    val editing: SavedTitle? = null,       // открыт диалог изменения
    val message: UserMessage? = null,
    val deleted: SavedTitle? = null,       // только что удалили — snackbar с «Отменить»
)

// Room отдаёт Flow: любое изменение таблицы (здесь или в поиске) сразу приходит сюда,
// stateIn превращает его в StateFlow для экрана
class SavedViewModel(
    private val repository: SavedRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val editing = MutableStateFlow<SavedTitle?>(null)
    private val message = MutableStateFlow<UserMessage?>(null)
    private val deleted = MutableStateFlow<SavedTitle?>(null)

    private val titles: Flow<UiState<List<SavedTitle>>> = repository.observeSaved()
        .map<List<SavedTitle>, UiState<List<SavedTitle>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(R.string.error_load_saved)) }

    // Список из Room + настройки из DataStore: поменялось любое — экран пересчитывается
    val state: StateFlow<SavedUiState> = combine(titles, preferences.savedPrefs, editing, message, deleted) {
            list, prefs, edit, msg, del ->
        SavedUiState(
            titles = when (list) {
                is UiState.Success -> UiState.Success(list.data.arrange(prefs))
                else -> list
            },
            total = (list as? UiState.Success)?.data?.size ?: 0,
            prefs = prefs,
            editing = edit,
            message = msg,
            deleted = del,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun startEdit(item: SavedTitle) {
        editing.value = item
    }

    fun dismissEdit() {
        editing.value = null
    }

    // Update: новая заметка и отметка «просмотрено». Список обновится сам — через Flow из Room.
    fun saveEdit(note: String, watched: Boolean) {
        val item = editing.value ?: return
        editing.value = null
        viewModelScope.launch {
            repository.update(item.copy(note = note.trim(), watched = watched))
            message.value = UserMessage(R.string.msg_updated, item.name)
        }
    }

    // Быстрое переключение «просмотрено» прямо в списке
    fun toggleWatched(item: SavedTitle) {
        viewModelScope.launch { repository.update(item.copy(watched = !item.watched)) }
    }

    // Delete: строка уходит из базы сразу, но копию держим — вдруг нажмут «Отменить»
    fun delete(item: SavedTitle) {
        editing.value = null
        viewModelScope.launch {
            repository.delete(item)
            deleted.value = item
        }
    }

    // Возвращаем ту же строку (с тем же id, заметкой и датой)
    fun undoDelete(item: SavedTitle) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun onDeletedShown() {
        deleted.value = null
    }

    // Настройки пишем в DataStore — после перезапуска приложения они останутся
    fun onSortChange(sort: SavedSort) {
        viewModelScope.launch { preferences.setSavedSort(sort) }
    }

    fun onHideWatchedChange(hide: Boolean) {
        viewModelScope.launch { preferences.setHideWatched(hide) }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun List<SavedTitle>.arrange(prefs: SavedPrefs): List<SavedTitle> {
        val visible = if (prefs.hideWatched) filterNot { it.watched } else this
        return when (prefs.sort) {
            SavedSort.NEWEST -> visible.sortedByDescending { it.addedAt }
            SavedSort.NAME -> visible.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SavedSort.YEAR -> visible.sortedWith(compareByDescending<SavedTitle> { it.year ?: 0 }.thenBy { it.name })
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SavedViewModel(app.container.savedRepository, app.container.preferencesRepository)
            }
        }
    }
}
