package com.habitchain.calendar.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.time.LocalDateProvider
import com.habitchain.calendar.domain.usecase.CalculateChains
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class GameViewModel(
    private val repository: HabitRepository,
    private val clock: LocalDateProvider,
    private val sample: (LocalDate) -> AppSnapshot,
    private val results: SessionResults
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<GameEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var advance: Job? = null
    private var active = false

    init {
        viewModelScope.launch {
            repository.state.collect { store ->
                val session = _uiState.value.session ?: return@collect
                if (session.mode == SessionMode.Real) {
                    val habits = session.habits.mapNotNull { old -> store.snapshot.habits.find { it.id == old.id } }
                    _uiState.update { it.copy(session = session.copy(habits = habits, histories = store.snapshot.histories), index = it.index.coerceIn(0, (habits.size - 1).coerceAtLeast(0)), newDay = clock.today() != session.date) }
                } else _uiState.update { it.copy(newDay = clock.today() != session.date) }
            }
        }
    }

    fun bind(id: String) {
        active = true
        if (_uiState.value.session?.id == id) { resume(); return }
        advance?.cancel()
        val stored = repository.state.value.snapshot
        val today = clock.today()
        val mode = if (stored.habits.isEmpty()) SessionMode.Sample else SessionMode.Real
        val snapshot = if (mode == SessionMode.Sample) sample(today) else stored
        val habits = snapshot.habits.sortedWith(compareBy<Habit> { it.createdOn }.thenBy { it.id })
        val completed = habits.filter { today in snapshot.history(it.id).completed }.map { it.id }.toSet()
        val session = CheckInSession(id, today, mode, habits, snapshot.histories, completed)
        _uiState.update { GameUiState(session, habits.indexOfFirst { it.id !in completed }.coerceAtLeast(0)) }
    }

    fun resume() { active = true; _uiState.update { it.copy(newDay = it.session?.date != clock.today()) } }
    fun pause() { active = false; advance?.cancel(); advance = null }
    fun picker(open: Boolean) { _uiState.update { it.copy(pickerOpen = open) } }
    fun select(index: Int) {
        if (_uiState.value.saving) return
        val count = _uiState.value.session?.habits?.size ?: return
        _uiState.update { it.copy(index = index.coerceIn(0, (count - 1).coerceAtLeast(0)), pickerOpen = false, error = false) }
    }

    fun mark() {
        val state = _uiState.value
        val session = state.session ?: return
        val habit = session.habits.getOrNull(state.index) ?: return
        if (state.saving || state.ended || !active || session.date != clock.today()) { resume(); return }
        if (session.date in (session.histories[habit.id]?.completed ?: emptySet())) return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                if (session.mode == SessionMode.Real) repository.mark(habit.id, session.date, true)
                _uiState.update { current ->
                    val latest = current.session ?: session
                    val old = latest.histories[habit.id] ?: HabitHistory()
                    current.copy(session = latest.copy(marked = latest.marked + habit.id, histories = latest.histories + (habit.id to old.copy(completed = old.completed + session.date))))
                }
                channel.send(GameEvent.Confirmed)
                _uiState.update { it.copy(saving = false) }
                if (active) scheduleAdvance(350L)
            } catch (_: Exception) { _uiState.update { it.copy(saving = false, error = true) } }
        }
    }

    fun skip() {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.saving || state.ended || !active) return
        val habit = session.habits.getOrNull(state.index) ?: return
        _uiState.update { it.copy(session = session.copy(skipped = session.skipped + habit.id)) }
        scheduleAdvance(180L)
    }

    private fun scheduleAdvance(wait: Long) {
        advance?.cancel()
        advance = viewModelScope.launch {
            delay(wait)
            if (!active) return@launch
            val session = _uiState.value.session ?: return@launch
            if (session.date != clock.today()) { _uiState.update { it.copy(newDay = true) }; return@launch }
            val next = session.habits.indexOfFirst { session.date !in (session.histories[it.id]?.completed ?: emptySet()) && it.id !in session.skipped }
            if (next < 0) finish() else _uiState.update { it.copy(index = next) }
        }
    }

    fun finish() {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.saving || state.ended || !active) return
        advance?.cancel()
        val completed = session.habits.filter { session.date in (session.histories[it.id]?.completed ?: emptySet()) }
        val changed = session.habits.filter { it.id in session.marked }
        val chain = changed.map { CalculateChains(session.histories[it.id]?.completed ?: emptySet(), session.date) }
        val result = ResultSnapshot(session.id, session.date, session.mode, completed.size, session.habits.size,
            session.completedAtStart.size.coerceAtMost(completed.size), changed.map { it.name }, session.habits.filterNot { it in completed }.map { it.name },
            chain.maxOfOrNull { it.current } ?: 0, chain.maxOfOrNull { it.longest } ?: 0)
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                if (session.mode == SessionMode.Real) repository.saveResult(result)
                results.put(result)
                _uiState.update { it.copy(ended = true, saving = false) }
                if (active) channel.send(GameEvent.Finished(session.id))
            } catch (_: Exception) { _uiState.update { it.copy(saving = false, error = true) } }
        }
    }
}
