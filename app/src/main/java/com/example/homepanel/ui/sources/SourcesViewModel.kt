package com.example.homepanel.ui.sources

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
import com.example.homepanel.data.model.OnlineResult
import com.example.homepanel.data.model.Release
import com.example.homepanel.data.model.TitleInfo
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.navigation.Routes
import com.example.homepanel.ui.toErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SourceTab { TRACKERS, ONLINE }

data class SourcesUiState(
    val heading: String = "",
    val info: UiState<TitleInfo> = UiState.Loading,
    val tab: SourceTab = SourceTab.TRACKERS,
    val releases: UiState<List<Release>> = UiState.Loading,
    val online: UiState<List<OnlineResult>> = UiState.Loading,
)

class SourcesViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
) : ViewModel() {

    // Аргументы маршрута sources/{type}/{tmdbId}?title={title}
    private val type = TitleType.valueOf(checkNotNull(savedStateHandle[Routes.ARG_TYPE]))
    private val tmdbId: Int = checkNotNull(savedStateHandle[Routes.ARG_TMDB_ID])
    private val title: String = savedStateHandle[Routes.ARG_TITLE] ?: ""

    private val _state = MutableStateFlow(SourcesUiState(heading = title))
    val state: StateFlow<SourcesUiState> = _state.asStateFlow()

    init {
        load()
    }

    // Сначала подробности тайтла, потом оба источника параллельно:
    // каждая вкладка заполняется, как только её источник ответил
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(info = UiState.Loading) }
            val info = try {
                repository.getTitleInfo(type, tmdbId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(info = UiState.Error(e.toErrorMessage(R.string.error_load_sources))) }
                return@launch
            }
            _state.update { it.copy(info = UiState.Success(info)) }
            loadReleases(info)
            loadOnline(info)
        }
    }

    fun retryReleases() = currentInfo()?.let { loadReleases(it) }

    fun retryOnline() = currentInfo()?.let { loadOnline(it) }

    fun onTabChange(tab: SourceTab) = _state.update { it.copy(tab = tab) }

    private fun loadReleases(info: TitleInfo) {
        viewModelScope.launch {
            _state.update { it.copy(releases = UiState.Loading) }
            val result = try {
                UiState.Success(repository.searchReleases(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toErrorMessage(R.string.error_load_sources))
            }
            _state.update { it.copy(releases = result) }
        }
    }

    private fun loadOnline(info: TitleInfo) {
        viewModelScope.launch {
            _state.update { it.copy(online = UiState.Loading) }
            val result = try {
                UiState.Success(repository.searchOnline(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toErrorMessage(R.string.error_load_sources))
            }
            _state.update { it.copy(online = result) }
        }
    }

    private fun currentInfo(): TitleInfo? = (_state.value.info as? UiState.Success)?.data

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SourcesViewModel(createSavedStateHandle(), app.container.repository)
            }
        }
    }
}
