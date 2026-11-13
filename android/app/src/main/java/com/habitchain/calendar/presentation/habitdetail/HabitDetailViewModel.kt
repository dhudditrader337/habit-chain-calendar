package com.habitchain.calendar.presentation.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.usecase.CalculateChains
import com.habitchain.calendar.domain.usecase.Chains
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitDetailUiState(
    val habit: Habit? = null,
    val history: HabitHistory = HabitHistory(),
    val chains: Chains = Chains(0, 0, 0),
    val today: LocalDate = LocalDate.now(),
    val defaultMinutes: Int = 1200,
    val loading: Boolean = true,
    val error: Boolean = false,
    val saving: Boolean = false,
    val deleteOpen: Boolean = false,
    val allNotes: Boolean = false,
    val noteDate: LocalDate? = null,
    val noteDraft: String = ""
)

enum class DetailEvent { Deleted, Saved }

class HabitDetailViewModel(private val repository: HabitRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<DetailEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var id: String? = null
    init { viewModelScope.launch { repository.state.collect { refresh() } } }
    fun bind(value: String) { if (id != value) { id = value; _uiState.update { HabitDetailUiState() } }; refresh() }
    private fun refresh() {
        val store = repository.state.value
        val history = store.snapshot.history(id.orEmpty())
        _uiState.update { it.copy(habit = store.snapshot.habits.find { habit -> habit.id == id }, history = history, chains = CalculateChains(history.completed, store.today), today = store.today, defaultMinutes = store.snapshot.settings.defaultReminderMinutes, loading = store.loading, error = store.error) }
    }
    fun retry() { viewModelScope.launch { repository.load() } }
    fun expandNotes() { _uiState.update { it.copy(allNotes = true) } }
    fun deleteDialog(open: Boolean) { _uiState.update { it.copy(deleteOpen = open) } }
    fun delete() {
        val habitId = id ?: return
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try { repository.deleteHabit(habitId); _uiState.update { it.copy(deleteOpen = false) }; channel.send(DetailEvent.Deleted) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
    fun openNote(date: LocalDate) { if (!date.isAfter(_uiState.value.today)) _uiState.update { it.copy(noteDate = date, noteDraft = it.history.notes[date].orEmpty()) } }
    fun note(value: String) { _uiState.update { it.copy(noteDraft = value.take(280)) } }
    fun closeNote() { if (!_uiState.value.saving) _uiState.update { it.copy(noteDate = null) } }
    fun saveNote() {
        val state = _uiState.value
        val date = state.noteDate ?: return
        val habitId = id ?: return
        if (state.saving) return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try { repository.saveNote(habitId, date, state.noteDraft); _uiState.update { it.copy(noteDate = null) }; channel.send(DetailEvent.Saved) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
}
