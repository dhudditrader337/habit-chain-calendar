package com.habitchain.calendar.domain.model

data class HabitSummary(
    val habit: Habit,
    val history: HabitHistory,
    val currentChain: Int,
    val longestChain: Int,
    val markedToday: Boolean
)
