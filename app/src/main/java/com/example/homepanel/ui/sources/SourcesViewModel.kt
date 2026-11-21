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
import com.example.homepanel.data.model.QualityProfile
import com.example.homepanel.data.model.Release
import com.example.homepanel.data.model.ReleaseNotFoundException
import com.example.homepanel.data.model.TitleInfo
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.UserMessage
import com.example.homepanel.ui.navigation.Routes
import com.example.homepanel.ui.toErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SourceTab { TRACKERS, ONLINE }

// Фильтры раздач: «Любое · 2160p · 1080p · ≤720p» и «Любая · Русская · Оригинал»
enum class QualityFilter { ANY, UHD, FHD, LOW }
enum class VoiceFilter { ANY, RUSSIAN, ORIGINAL }

data class SourcesUiState(
    val heading: String = "",
    val info: UiState<TitleInfo> = UiState.Loading,
    val tab: SourceTab = SourceTab.TRACKERS,
    val releases: UiState<List<Release>> = UiState.Loading,
    val online: UiState<List<OnlineResult>> = UiState.Loading,
    val quality: QualityFilter = QualityFilter.ANY,
    val voice: VoiceFilter = VoiceFilter.ANY,
    val grab: GrabDialogState? = null,     // открыт диалог «Скачать?»
    val grabbing: String? = null,          // идёт передача раздачи: имя сервиса (Radarr/Sonarr)
    val message: UserMessage? = null,
) {
    // Раздачи под текущие фильтры (фильтруем на телефоне — повторный поиск не нужен)
    val visibleReleases: List<Release>
        get() = (releases as? UiState.Success)?.data.orEmpty().filter { it.matches(quality, voice) }
}

// Диалог скачивания. profiles == null — тайтл уже в Radarr/Sonarr, профиль не нужен.
data class GrabDialogState(
    val release: Release,
    val service: String,
    val checking: Boolean = true,
    val profiles: List<QualityProfile>? = null,
    val selectedProfile: Int? = null,
)

private fun Release.matches(quality: QualityFilter, voice: VoiceFilter): Boolean {
    val qualityOk = when (quality) {
        QualityFilter.ANY -> true
        QualityFilter.UHD -> resolution == "2160p"
        QualityFilter.FHD -> resolution == "1080p"
        QualityFilter.LOW -> resolution == "720p" || resolution == "SD"
    }
    val voiceOk = when (voice) {
        VoiceFilter.ANY -> true
        VoiceFilter.RUSSIAN -> hasRussian
        VoiceFilter.ORIGINAL -> hasOriginal
    }
    return qualityOk && voiceOk
}

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

    fun onQualityChange(q: QualityFilter) = _state.update { it.copy(quality = q) }

    fun onVoiceChange(v: VoiceFilter) = _state.update { it.copy(voice = v) }

    // Тап по раздаче: узнаём, есть ли тайтл в Radarr/Sonarr; если нет — нужен профиль качества
    fun onReleaseClick(release: Release) {
        val info = currentInfo() ?: return
        val service = if (info.type == TitleType.MOVIE) "Radarr" else "Sonarr"
        _state.update { it.copy(grab = GrabDialogState(release, service)) }
        viewModelScope.launch {
            val dialog = try {
                if (repository.isInArr(info)) {
                    GrabDialogState(release, service, checking = false)
                } else {
                    val profiles = repository.getProfiles(info.type)
                    // По умолчанию — профиль с русской озвучкой, если он есть
                    val preferred = profiles.firstOrNull { "RU" in it.name } ?: profiles.firstOrNull()
                    GrabDialogState(release, service, checking = false, profiles = profiles, selectedProfile = preferred?.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(grab = null, message = UserMessage(e.toErrorMessage(R.string.msg_grab_failed))) }
                return@launch
            }
            _state.update { if (it.grab?.release == release) it.copy(grab = dialog) else it }
        }
    }

    fun onProfileSelect(id: Int) = _state.update { s -> s.copy(grab = s.grab?.copy(selectedProfile = id)) }

    fun dismissGrab() = _state.update { it.copy(grab = null) }

    // Долго (Radarr/Sonarr ищут раздачу сами) — экран не блокируем, показываем строку прогресса
    fun confirmGrab() {
        val info = currentInfo() ?: return
        val dialog = _state.value.grab ?: return
        _state.update { it.copy(grab = null, grabbing = dialog.service) }
        viewModelScope.launch {
            val message = try {
                repository.grabRelease(info, dialog.release, dialog.selectedProfile)
                UserMessage(R.string.msg_grabbed, info.name.ifBlank { title })
            } catch (e: CancellationException) {
                throw e
            } catch (e: ReleaseNotFoundException) {
                UserMessage(R.string.msg_release_not_found)
            } catch (e: Exception) {
                UserMessage(e.toErrorMessage(R.string.msg_grab_failed))
            }
            _state.update { it.copy(grabbing = null, message = message) }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

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
