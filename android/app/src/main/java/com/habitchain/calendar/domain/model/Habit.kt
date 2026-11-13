package com.habitchain.calendar.domain.model

import java.time.LocalDate

enum class HabitColor { Leaf, Sky, Forest, Ink }

data class Habit(
    val id: String,
    val name: String,
    val color: HabitColor,
    val createdOn: LocalDate,
    val reminderEnabled: Boolean = false,
    val customReminderMinutes: Int? = null
)

data class HabitHistory(
    val completed: Set<LocalDate> = emptySet(),
    val notes: Map<LocalDate, String> = emptyMap()
)

data class AppSettings(val defaultReminderMinutes: Int = 1200, val remindersEnabled: Boolean = false)

data class AppSnapshot(
    val habits: List<Habit> = emptyList(),
    val histories: Map<String, HabitHistory> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val lastResult: ResultSnapshot? = null,
    val feedback: String = ""
) {
    fun history(id: String) = histories[id] ?: HabitHistory()
}

data class StoreState(
    val snapshot: AppSnapshot = AppSnapshot(),
    val loading: Boolean = true,
    val error: Boolean = false,
    val today: LocalDate = LocalDate.now()
)
