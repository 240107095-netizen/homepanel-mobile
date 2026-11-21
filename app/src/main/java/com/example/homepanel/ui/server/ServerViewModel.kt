package com.example.homepanel.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.MediaRepository
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.UserMessage
import com.example.homepanel.ui.toErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerUiState(
    val status: UiState<ServerStatus> = UiState.Loading,
    val rescanning: Boolean = false,
    val message: UserMessage? = null,
)

class ServerViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(status = UiState.Loading) }
            val result = try {
                UiState.Success(repository.getServerStatus())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toErrorMessage(R.string.error_load_server))
            }
            _state.update { it.copy(status = result) }
        }
    }

    // Обратимо и безопасно — запускаем сразу, кнопка неактивна, пока идёт запрос
    fun rescanLibrary() {
        if (_state.value.rescanning) return
        viewModelScope.launch {
            _state.update { it.copy(rescanning = true) }
            val message = try {
                repository.refreshLibrary()
                UserMessage(R.string.msg_rescan_started)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UserMessage(R.string.msg_action_failed)
            }
            _state.update { it.copy(rescanning = false, message = message) }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                ServerViewModel(app.container.repository)
            }
        }
    }
}
