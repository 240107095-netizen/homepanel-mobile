package com.example.homepanel.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.MediaRepository
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.model.TorrentState
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.toErrorMessage
import com.example.homepanel.ui.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val torrents: UiState<List<Torrent>> = UiState.Loading,
    val message: UserMessage? = null,
    val stale: Boolean = false,  // связь пропала: показываем последние известные данные
    val isRefreshing: Boolean = false,
) {
    // Общая скорость для шапки
    val totalSpeed: Long
        get() = (torrents as? UiState.Success)?.data?.sumOf { it.downloadSpeed } ?: 0
}

class DownloadsViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())

    // Пока экран подписан на state, раз в 2 секунды спрашиваем сервер.
    // Экран ушёл (или приложение свернули) — через 5 секунд опрос останавливается сам.
    val state: StateFlow<DownloadsUiState> = channelFlow {
        launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
        _state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    // Кнопка «Повторить» на экране ошибки
    fun load() {
        _state.update { it.copy(torrents = UiState.Loading) }
        viewModelScope.launch { refresh() }
    }

    // Свайп вниз: не ждём следующего тика опроса
    fun pullRefresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            refresh()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun refresh() {
        try {
            val list = repository.getTorrents()
            _state.update { it.copy(torrents = UiState.Success(list), stale = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Данные уже были — не стираем их, а помечаем устаревшими
            _state.update {
                if (it.torrents is UiState.Success) it.copy(stale = true)
                else it.copy(torrents = UiState.Error(e.toErrorMessage(R.string.error_load_downloads)))
            }
        }
    }

    // Пауза/старт — обратимое действие, поэтому сразу и без вопросов.
    // Сначала меняем экран (оптимистично), если сервер отказал — откатываем.
    fun toggle(torrent: Torrent) {
        val pause = torrent.state == TorrentState.DOWNLOADING
        val newState = if (pause) TorrentState.PAUSED else TorrentState.DOWNLOADING
        replace(torrent.copy(state = newState, downloadSpeed = 0))
        viewModelScope.launch {
            try {
                if (pause) repository.pauseTorrent(torrent.hash) else repository.resumeTorrent(torrent.hash)
                showMessage(UserMessage(if (pause) R.string.msg_paused else R.string.msg_resumed, torrent.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                replace(torrent)
                showMessage(UserMessage(R.string.msg_action_failed))
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun showMessage(message: UserMessage) {
        _state.update { it.copy(message = message) }
    }

    private fun replace(torrent: Torrent) {
        _state.update { s ->
            val list = (s.torrents as? UiState.Success)?.data ?: return@update s
            s.copy(torrents = UiState.Success(list.map { if (it.hash == torrent.hash) torrent else it }))
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                DownloadsViewModel(app.container.repository)
            }
        }
    }
}
