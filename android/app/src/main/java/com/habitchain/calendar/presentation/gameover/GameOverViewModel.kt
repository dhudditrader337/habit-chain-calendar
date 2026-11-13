package com.habitchain.calendar.presentation.gameover

import androidx.lifecycle.ViewModel
import com.habitchain.calendar.domain.model.ResultSnapshot
import com.habitchain.calendar.domain.model.SessionResults
import com.habitchain.calendar.domain.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameOverUiState(val result: ResultSnapshot? = null)

class GameOverViewModel(private val results: SessionResults, private val repository: HabitRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GameOverUiState())
    val uiState = _uiState.asStateFlow()
    fun bind(id: String) {
        _uiState.update { it.copy(result = results.get(id) ?: repository.state.value.snapshot.lastResult?.takeIf { result -> result.sessionId == id }) }
    }
}
