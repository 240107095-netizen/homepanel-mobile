package com.example.homepanel.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.MediaRepository
import com.example.homepanel.data.model.Title
import com.example.homepanel.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Логика экрана библиотеки. Экран только читает state и вызывает функции.
class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Title>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Title>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.getLibrary().sortedBy { it.name })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(R.string.error_load_library)
            }
        }
    }

    companion object {
        // Фабрика берёт репозиторий из контейнера приложения
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                LibraryViewModel(app.container.repository)
            }
        }
    }
}
