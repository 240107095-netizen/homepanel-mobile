package com.example.homepanel.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homepanel.HomepanelApplication
import com.example.homepanel.R
import com.example.homepanel.data.ScheduleRepository
import com.example.homepanel.data.TvMazeUnavailableException
import com.example.homepanel.data.model.ShowSchedule
import com.example.homepanel.data.model.ShowStatus
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.toErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Цепочка: Retrofit (TvMazeApi) → ScheduleRepository → эта ViewModel → StateFlow → Compose-экран
class UpcomingViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ShowSchedule>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ShowSchedule>>> = _state.asStateFlow()

    init {
        load()
    }

    // И первая загрузка, и кнопка «Повторить»
    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.getSchedules().sortedWith(ORDER))
            } catch (e: CancellationException) {
                throw e
            } catch (e: TvMazeUnavailableException) {
                UiState.Error(R.string.error_tvmaze)
            } catch (e: Exception) {
                // Сюда попадает и ошибка домашнего сервера (список сериалов берём из Jellyfin)
                UiState.Error(e.toErrorMessage(R.string.error_load_upcoming))
            }
        }
    }

    companion object {
        // Сверху те, что отстают (больше пропущенных — выше), потом с известной датой (ближайшие выше),
        // потом идущие без даты, завершённые — в конце
        private val ORDER = compareByDescending<ShowSchedule> { it.missing }
            .thenByDescending { it.newSeasonEpisodes }
            .thenBy { it.next == null }
            .thenBy { it.next?.airdate.orEmpty() }
            .thenBy { it.status == ShowStatus.ENDED }
            .thenBy { it.name }

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HomepanelApplication
                UpcomingViewModel(app.container.scheduleRepository)
            }
        }
    }
}
