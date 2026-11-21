package com.example.homepanel.ui.title

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.MediaRepository
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TitleDetails(val title: Title, val seasons: List<Season>)

class TitleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
) : ViewModel() {

    // Аргумент маршрута title/{itemId}: Navigation кладёт его в SavedStateHandle
    private val itemId: String = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID])

    private val _state = MutableStateFlow<UiState<TitleDetails>>(UiState.Loading)
    val state: StateFlow<UiState<TitleDetails>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val title = repository.getTitle(itemId)
                // Сезоны есть только у сериалов
                val seasons = if (title.type == TitleType.SERIES) repository.getSeasons(itemId) else emptyList()
                UiState.Success(TitleDetails(title, seasons))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(R.string.error_load_title)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                TitleViewModel(createSavedStateHandle(), app.container.repository)
            }
        }
    }
}
