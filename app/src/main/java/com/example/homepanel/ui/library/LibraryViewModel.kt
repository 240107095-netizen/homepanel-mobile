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
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.toErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Фильтр по типу: вкладки «Все · Фильмы · Сериалы»
enum class LibraryFilter { ALL, MOVIES, SERIES }

// Всё, что рисует экран библиотеки
data class LibraryUiState(
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val total: Int = 0,                                  // сколько тайтлов всего, до фильтра
    val titles: UiState<List<Title>> = UiState.Loading,  // уже отфильтрованные
)

// Логика экрана библиотеки. Экран только читает state и вызывает функции.
class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val library = MutableStateFlow<UiState<List<Title>>>(UiState.Loading)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)

    // Любое изменение (данные, текст поиска, вкладка) пересчитывает state
    val state: StateFlow<LibraryUiState> = combine(library, query, filter) { lib, q, f ->
        LibraryUiState(
            query = q,
            filter = f,
            total = (lib as? UiState.Success)?.data?.size ?: 0,
            titles = when (lib) {
                is UiState.Success -> UiState.Success(lib.data.filter { it.matches(q, f) })
                else -> lib
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            library.value = UiState.Loading
            library.value = try {
                UiState.Success(repository.getLibrary().sortedBy { it.name })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toErrorMessage(R.string.error_load_library))
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: LibraryFilter) {
        filter.value = value
    }

    // Ищем и по русскому, и по оригинальному названию
    private fun Title.matches(q: String, f: LibraryFilter): Boolean {
        val typeOk = when (f) {
            LibraryFilter.ALL -> true
            LibraryFilter.MOVIES -> type == TitleType.MOVIE
            LibraryFilter.SERIES -> type == TitleType.SERIES
        }
        val text = q.trim()
        return typeOk && (text.isEmpty() || name.contains(text, true) || originalName.contains(text, true))
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
