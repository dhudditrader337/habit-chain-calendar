package com.habitchain.calendar.domain.repository

import com.habitchain.calendar.domain.model.*
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface HabitRepository {
    val state: StateFlow<StoreState>
    suspend fun load()
    suspend fun refreshDate()
    suspend fun saveHabit(habit: Habit)
    suspend fun mark(id: String, date: LocalDate, completed: Boolean)
    suspend fun saveNote(id: String, date: LocalDate, note: String)
    suspend fun deleteHabit(id: String)
    suspend fun saveSettings(settings: AppSettings)
    suspend fun saveResult(result: ResultSnapshot)
    suspend fun saveFeedback(message: String)
    suspend fun reset()
}
