package com.habitchain.calendar.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val habit: Habit? = null,
    val history: HabitHistory = HabitHistory(),
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false,
    val confirmRemove: Boolean = false,
    val noteDraft: String? = null
) {
    val eligible: Boolean get() = habit?.let { !selected.isBefore(it.createdOn) && !selected.isAfter(today) } ?: false
    val marked: Boolean get() = selected in history.completed
}

enum class CalendarEvent { Saved, Removed }

class CalendarViewModel(private val repository: HabitRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<CalendarEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var id: String? = null
    private var removedDate: LocalDate? = null
    init {
        viewModelScope.launch { repository.state.collect { store ->
            _uiState.update { it.copy(habit = store.snapshot.habits.find { habit -> habit.id == id }, history = store.snapshot.history(id.orEmpty()), today = store.today, loading = store.loading, error = store.error) }
        } }
    }
    fun bind(habitId: String) {
        val store = repository.state.value
        if (id != habitId) {
            id = habitId
            _uiState.update { CalendarUiState(habit = store.snapshot.habits.find { it.id == id }, history = store.snapshot.history(habitId), month = YearMonth.from(store.today), today = store.today, selected = store.today, loading = store.loading, error = store.error) }
        }
    }
    fun month(offset: Long) { _uiState.update { it.copy(month = it.month.plusMonths(offset)) } }
    fun today() { _uiState.update { it.copy(month = YearMonth.from(it.today), selected = it.today) } }
    fun select(date: LocalDate) { if (!_uiState.value.saving) _uiState.update { it.copy(selected = date, noteDraft = null) } }
    fun retry() { viewModelScope.launch { repository.load() } }
    fun markAction() {
        if (!_uiState.value.eligible || _uiState.value.saving) return
        if (_uiState.value.marked) _uiState.update { it.copy(confirmRemove = true) } else writeMark(true)
    }
    fun dismissRemove() { _uiState.update { it.copy(confirmRemove = false) } }
    fun remove() { _uiState.update { it.copy(confirmRemove = false) }; writeMark(false) }
    private fun writeMark(mark: Boolean, date: LocalDate = _uiState.value.selected) {
        val habitId = id ?: return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                repository.mark(habitId, date, mark)
                if (!mark) removedDate = date
                channel.send(if (mark) CalendarEvent.Saved else CalendarEvent.Removed)
            } catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
    fun undo() { removedDate?.let { writeMark(true, it) }; removedDate = null }
    fun openNote() { if (_uiState.value.eligible) _uiState.update { it.copy(noteDraft = it.history.notes[it.selected].orEmpty()) } }
    fun note(value: String) { _uiState.update { it.copy(noteDraft = value.take(280)) } }
    fun closeNote() { if (!_uiState.value.saving) _uiState.update { it.copy(noteDraft = null) } }
    fun saveNote() {
        val state = _uiState.value
        val habitId = id ?: return
        val note = state.noteDraft ?: return
        if (state.saving) return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try { repository.saveNote(habitId, state.selected, note); _uiState.update { it.copy(noteDraft = null) }; channel.send(CalendarEvent.Saved) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
}
