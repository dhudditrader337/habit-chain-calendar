package com.habitchain.calendar.presentation.game

import com.habitchain.calendar.domain.model.CheckInSession

data class GameUiState(
    val session: CheckInSession? = null,
    val index: Int = 0,
    val saving: Boolean = false,
    val error: Boolean = false,
    val ended: Boolean = false,
    val newDay: Boolean = false,
    val pickerOpen: Boolean = false
)

sealed interface GameEvent {
    data object Confirmed : GameEvent
    data class Finished(val sessionId: String) : GameEvent
}
