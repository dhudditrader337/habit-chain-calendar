package com.habitchain.calendar.presentation.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.domain.repository.HabitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AboutUiState(
    val version: String = "",
    val privacyExpanded: Boolean = false,
    val contactOpen: Boolean = false,
    val draft: String = "",
    val saving: Boolean = false,
    val error: Boolean = false,
    val copied: Boolean = false,
    val copyError: Boolean = false
)

sealed interface AboutEvent {
    data class Copy(val message: String) : AboutEvent
    data object Saved : AboutEvent
}

class AboutViewModel(private val repository: HabitRepository, version: String) : ViewModel() {
    private val _uiState = MutableStateFlow(AboutUiState(version = version))
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<AboutEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    fun bind(privacy: Boolean) { if (privacy) _uiState.update { it.copy(privacyExpanded = true) } }
    fun privacy() { _uiState.update { it.copy(privacyExpanded = !it.privacyExpanded) } }
    fun contact() { _uiState.update { it.copy(contactOpen = true, draft = repository.state.value.snapshot.feedback, error = false, copied = false, copyError = false) } }
    fun closeContact() { if (!_uiState.value.saving) _uiState.update { it.copy(contactOpen = false) } }
    fun draft(value: String) { _uiState.update { it.copy(draft = value.take(500), copied = false, copyError = false) } }
    fun copy() { viewModelScope.launch { channel.send(AboutEvent.Copy(_uiState.value.draft)) } }
    fun copied(success: Boolean) { _uiState.update { it.copy(copied = success, copyError = !success) } }
    fun save() {
        if (_uiState.value.saving) return
        val message = _uiState.value.draft
        _uiState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try { repository.saveFeedback(message); channel.send(AboutEvent.Saved) }
            catch (_: Exception) { _uiState.update { it.copy(error = true) } }
            _uiState.update { it.copy(saving = false) }
        }
    }
}
