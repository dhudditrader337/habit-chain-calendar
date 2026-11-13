package com.habitchain.calendar.domain.model

import java.time.LocalDate

enum class SessionMode { Real, Sample }

data class ResultSnapshot(
    val sessionId: String,
    val date: LocalDate,
    val mode: SessionMode,
    val markedCount: Int,
    val totalCount: Int,
    val alreadyCount: Int,
    val changedNames: List<String>,
    val pendingNames: List<String>,
    val currentChain: Int,
    val longestChain: Int
)

data class CheckInSession(
    val id: String,
    val date: LocalDate,
    val mode: SessionMode,
    val habits: List<Habit>,
    val histories: Map<String, HabitHistory>,
    val completedAtStart: Set<String>,
    val marked: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet()
)
