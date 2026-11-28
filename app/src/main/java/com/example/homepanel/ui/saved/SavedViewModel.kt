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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SavedUiState(
    val titles: UiState<List<SavedTitle>> = UiState.Loading,
)

// Room отдаёт Flow: любое изменение таблицы (здесь или в поиске) сразу приходит сюда,
// stateIn превращает его в StateFlow для экрана
class SavedViewModel(private val repository: SavedRepository) : ViewModel() {

    val state: StateFlow<SavedUiState> = repository.observeSaved()
        .map { SavedUiState(UiState.Success(it)) }
        .catch { emit(SavedUiState(UiState.Error(R.string.error_load_saved))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SavedViewModel(app.container.savedRepository)
            }
        }
    }
}
