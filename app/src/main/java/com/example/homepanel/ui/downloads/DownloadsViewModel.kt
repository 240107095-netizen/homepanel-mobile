package com.example.homepanel.ui.downloads

import androidx.annotation.StringRes
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Короткое сообщение внизу экрана (snackbar): строка + имя раздачи
data class UserMessage(@StringRes val text: Int, val arg: String? = null)

data class DownloadsUiState(
    val torrents: UiState<List<Torrent>> = UiState.Loading,
    val message: UserMessage? = null,
) {
    // Общая скорость для шапки
    val totalSpeed: Long
        get() = (torrents as? UiState.Success)?.data?.sumOf { it.downloadSpeed } ?: 0
}

class DownloadsViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(torrents = UiState.Loading) }
            val result = try {
                UiState.Success(repository.getTorrents())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(R.string.error_load_downloads)
            }
            _state.update { it.copy(torrents = result) }
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
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                DownloadsViewModel(app.container.repository)
            }
        }
    }
}
