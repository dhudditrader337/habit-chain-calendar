package com.habitchain.calendar.presentation.addhabit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.time.LocalDateProvider
import com.habitchain.calendar.domain.usecase.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class AddHabitUiState(
    val editingId: String? = null,
    val name: String = "",
    val color: HabitColor = HabitColor.Leaf,
    val reminder: Boolean = false,
    val customMinutes: Int? = null,
    val defaultMinutes: Int = 1200,
    val timeDraft: Int? = null,
    val nameIssue: NameIssue? = null,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val error: Boolean = false,
    val discardOpen: Boolean = false,
    val notificationsAllowed: Boolean = false
)

enum class AddHabitEvent { Saved, Back, RequestPermission, OpenNotificationSettings }

class AddHabitViewModel(private val repository: HabitRepository, private val clock: LocalDateProvider, private val notificationsAllowed: () -> Boolean) : ViewModel() {
    private val _uiState = MutableStateFlow(AddHabitUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<AddHabitEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var draftId: String? = null

    fun bind(token: String, id: String?) {
        if (draftId == token) { refreshPermission(); return }
        draftId = token
        val snapshot = repository.state.value.snapshot
        val habit = snapshot.habits.find { it.id == id }
        _uiState.update { AddHabitUiState(editingId = habit?.id, name = habit?.name.orEmpty(), color = habit?.color ?: HabitColor.Leaf,
            reminder = habit?.reminderEnabled ?: false, customMinutes = habit?.customReminderMinutes,
            defaultMinutes = snapshot.settings.defaultReminderMinutes, notificationsAllowed = notificationsAllowed()) }
    }

    fun refreshPermission() {
        _uiState.update { it.copy(notificationsAllowed = notificationsAllowed()) }
        viewModelScope.launch { repository.refreshDate() }
    }
    fun name(value: String) { if (!_uiState.value.saving) _uiState.update { it.copy(name = value.take(40), dirty = true, nameIssue = null) } }
    fun color(value: HabitColor) { _uiState.update { it.copy(color = value, dirty = true) } }
    fun reminder(value: Boolean) {
        _uiState.update { it.copy(reminder = value, dirty = true) }
        if (value && !notificationsAllowed()) viewModelScope.launch { channel.send(AddHabitEvent.RequestPermission) }
    }
    fun notificationSettings() { viewModelScope.launch { channel.send(AddHabitEvent.OpenNotificationSettings) } }
    fun defaultTime() { _uiState.update { it.copy(customMinutes = null, dirty = true) } }
    fun openTime() { _uiState.update { it.copy(timeDraft = it.customMinutes ?: it.defaultMinutes) } }
    fun stepTime(minutes: Int) { _uiState.update { it.copy(timeDraft = Math.floorMod((it.timeDraft ?: 1200) + minutes, 1440)) } }
    fun closeTime() { _uiState.update { it.copy(timeDraft = null) } }
    fun confirmTime() { _uiState.update { it.copy(customMinutes = it.timeDraft, timeDraft = null, dirty = true) } }
    fun back() {
        if (_uiState.value.saving) return
        if (_uiState.value.dirty) _uiState.update { it.copy(discardOpen = true) }
        else viewModelScope.launch { channel.send(AddHabitEvent.Back) }
    }
    fun dismissDiscard() { _uiState.update { it.copy(discardOpen = false) } }
    fun discard() { _uiState.update { it.copy(discardOpen = false, dirty = false) }; viewModelScope.launch { channel.send(AddHabitEvent.Back) } }
    fun save() {
        val state = _uiState.value
        if (state.saving) return
        val snapshot = repository.state.value.snapshot
        val issue = ValidateHabit(state.name, state.editingId, snapshot.habits)
        if (issue != null) { _uiState.update { it.copy(nameIssue = issue) }; return }
        val old = snapshot.habits.find { it.id == state.editingId }
        val habit = Habit(old?.id ?: UUID.randomUUID().toString(), state.name.trim(), state.color, old?.createdOn ?: clock.today(), state.reminder, state.customMinutes)
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                repository.saveHabit(habit)
                _uiState.update { it.copy(saving = false, dirty = false) }
                channel.send(AddHabitEvent.Saved)
            } catch (_: Exception) { _uiState.update { it.copy(saving = false, error = true) } }
        }
    }
}
