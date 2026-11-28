package com.example.homepanel.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.SavedRepository
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
    val titles: UiState<List<SavedTitle>> = UiState.Loading,
    val editing: SavedTitle? = null,       // открыт диалог изменения
    val message: UserMessage? = null,
)

// Room отдаёт Flow: любое изменение таблицы (здесь или в поиске) сразу приходит сюда,
// stateIn превращает его в StateFlow для экрана
class SavedViewModel(private val repository: SavedRepository) : ViewModel() {

    private val editing = MutableStateFlow<SavedTitle?>(null)
    private val message = MutableStateFlow<UserMessage?>(null)

    private val titles: Flow<UiState<List<SavedTitle>>> = repository.observeSaved()
        .map<List<SavedTitle>, UiState<List<SavedTitle>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(R.string.error_load_saved)) }

    val state: StateFlow<SavedUiState> = combine(titles, editing, message) { list, edit, msg ->
        SavedUiState(list, edit, msg)
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

    fun onMessageShown() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SavedViewModel(app.container.savedRepository)
            }
        }
    }
}
