package com.habitchain.calendar.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.AppSnapshot
import com.habitchain.calendar.domain.model.HabitSummary
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.usecase.CalculateChains
import com.habitchain.calendar.domain.time.LocalDateProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MenuEvent { Confirmed, Removed }

class MenuViewModel(private val repository: HabitRepository, private val sample: (LocalDate) -> AppSnapshot, private val clock: LocalDateProvider) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<MenuEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var removed: Pair<String, LocalDate>? = null
    init {
        viewModelScope.launch {
            repository.state.collect { store ->
                fun summaries(snapshot: AppSnapshot) = snapshot.habits.map { habit ->
                    val history = snapshot.history(habit.id)
                    val chains = CalculateChains(history.completed, store.today)
                    HabitSummary(habit, history, chains.current, chains.longest, store.today in history.completed)
                }
                _uiState.update { it.copy(loading = store.loading, today = store.today, habits = summaries(store.snapshot), examples = summaries(sample(store.today)), error = store.error) }
            }
        }
    }
    fun retry() { viewModelScope.launch { repository.load() } }
    fun toggle(id: String) {
        if (_uiState.value.savingId != null) return
        val summary = _uiState.value.habits.firstOrNull { it.habit.id == id } ?: return
        val today = clock.today()
        val marked = today in summary.history.completed
        _uiState.update { it.copy(savingId = id, saveError = false) }
        viewModelScope.launch {
            try {
                repository.refreshDate()
                repository.mark(id, today, !marked)
                if (marked) { removed = id to today; channel.send(MenuEvent.Removed) } else channel.send(MenuEvent.Confirmed)
            } catch (_: Exception) { _uiState.update { it.copy(saveError = true) } }
            _uiState.update { it.copy(savingId = null) }
        }
    }
    fun undo() {
        val (id, date) = removed ?: return
        removed = null
        viewModelScope.launch {
            try { repository.mark(id, date, true); channel.send(MenuEvent.Confirmed) }
            catch (_: Exception) { _uiState.update { it.copy(saveError = true) } }
        }
    }
}
