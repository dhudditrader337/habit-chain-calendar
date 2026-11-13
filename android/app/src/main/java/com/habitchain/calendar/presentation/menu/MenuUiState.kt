package com.habitchain.calendar.presentation.menu

import com.habitchain.calendar.domain.model.HabitSummary
import java.time.LocalDate

data class MenuUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val habits: List<HabitSummary> = emptyList(),
    val examples: List<HabitSummary> = emptyList(),
    val error: Boolean = false,
    val saveError: Boolean = false,
    val savingId: String? = null
) {
    val doneToday: Int get() = habits.count { it.markedToday }
    val activeChains: Int get() = habits.count { it.currentChain > 0 }
}
