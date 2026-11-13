package com.habitchain.calendar.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.model.AppSettings
import com.habitchain.calendar.domain.model.SessionResults
import com.habitchain.calendar.domain.repository.HabitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val notificationsAllowed: Boolean = false,
    val timeDraft: Int? = null,
    val saving: Boolean = false,
    val error: Boolean = false,
    val resetStep: Int = 0
)

enum class SettingsEvent { Saved, Reset, RequestPermission, NotificationSettings }

class SettingsViewModel(private val repository: HabitRepository, private val allowed: () -> Boolean, private val results: SessionResults) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var pendingSettings: AppSettings? = null
    init { viewModelScope.launch { repository.state.collect { store -> _uiState.update { it.copy(settings = store.snapshot.settings, error = store.error, notificationsAllowed = allowed()) } } } }
    fun refresh() {
        _uiState.update { it.copy(notificationsAllowed = allowed()) }
        viewModelScope.launch { repository.refreshDate() }
    }
    fun reminders(enabled: Boolean) {
        save(_uiState.value.settings.copy(remindersEnabled = enabled))
        if (enabled && !allowed()) viewModelScope.launch { channel.send(SettingsEvent.RequestPermission) }
    }
    fun manage() { viewModelScope.launch { channel.send(SettingsEvent.NotificationSettings) } }
    fun openTime() { _uiState.update { it.copy(timeDraft = it.settings.defaultReminderMinutes) } }
    fun stepTime(change: Int) { _uiState.update { it.copy(timeDraft = Math.floorMod((it.timeDraft ?: 1200) + change, 1440)) } }
    fun closeTime() { _uiState.update { it.copy(timeDraft = null) } }
    fun confirmTime() { val time = _uiState.value.timeDraft ?: return; save(_uiState.value.settings.copy(defaultReminderMinutes = time)) }
    private fun save(settings: AppSettings) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, error = false) }
        pendingSettings = settings
        viewModelScope.launch {
            try { repository.saveSettings(settings); pendingSettings = null; _uiState.update { it.copy(timeDraft = null) }; channel.send(SettingsEvent.Saved) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
    fun resetDialog(step: Int) { if (!_uiState.value.saving) _uiState.update { it.copy(resetStep = step) } }
    fun reset() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try { repository.reset(); results.clear(); _uiState.update { it.copy(resetStep = 0) }; channel.send(SettingsEvent.Reset) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
    fun retry() {
        val pending = pendingSettings
        if (pending != null) save(pending) else viewModelScope.launch { repository.load() }
    }
}
