package com.example.homepanel.ui.season

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
import com.example.homepanel.data.model.Episode
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeasonViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
) : ViewModel() {

    // Оба аргумента маршрута season/{seriesId}/{seasonId}
    private val seriesId: String = checkNotNull(savedStateHandle[Routes.ARG_SERIES_ID])
    private val seasonId: String = checkNotNull(savedStateHandle[Routes.ARG_SEASON_ID])

    private val _state = MutableStateFlow<UiState<List<Episode>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Episode>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.getEpisodes(seriesId, seasonId).sortedBy { it.number })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(R.string.error_load_season)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SeasonViewModel(createSavedStateHandle(), app.container.repository)
            }
        }
    }
}
