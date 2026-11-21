package com.example.homepanel.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.MediaRepository
import com.example.homepanel.data.model.MediaStatus
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: UiState<List<SearchResult>>? = null,  // null — ещё ничего не искали
    val confirm: SearchResult? = null,                 // открыт диалог «Запросить?»
    val message: UserMessage? = null,
)

class SearchViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        observeQuery()
    }

    // Ищем, когда пользователь перестал печатать на 400 мс.
    // collectLatest отменяет старый запрос, если текст снова изменился.
    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            _state.map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(400)
                .collectLatest { query -> search(query) }
        }
    }

    private suspend fun search(query: String) {
        if (query.isEmpty()) {
            _state.update { it.copy(results = null) }
            return
        }
        _state.update { it.copy(results = UiState.Loading) }
        val result = try {
            UiState.Success(repository.search(query))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(R.string.error_search)
        }
        _state.update { it.copy(results = result) }
    }

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun retry() {
        viewModelScope.launch { search(_state.value.query.trim()) }
    }

    // Запрос необратим (сервер начнёт качать), поэтому сначала подтверждение
    fun askRequest(item: SearchResult) = _state.update { it.copy(confirm = item) }

    fun dismissRequest() = _state.update { it.copy(confirm = null) }

    fun confirmRequest() {
        val item = _state.value.confirm ?: return
        _state.update { it.copy(confirm = null) }
        viewModelScope.launch {
            try {
                repository.request(item)
                setStatus(item, MediaStatus.REQUESTED)
                _state.update { it.copy(message = UserMessage(R.string.msg_requested, item.name)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = UserMessage(R.string.msg_action_failed)) }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    private fun setStatus(item: SearchResult, status: MediaStatus) {
        _state.update { s ->
            val list = (s.results as? UiState.Success)?.data ?: return@update s
            s.copy(results = UiState.Success(list.map {
                if (it.tmdbId == item.tmdbId && it.type == item.type) it.copy(status = status) else it
            }))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                SearchViewModel(app.container.repository)
            }
        }
    }
}
