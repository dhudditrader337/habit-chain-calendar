package com.habitchain.calendar.domain.usecase

import com.habitchain.calendar.domain.model.Habit

enum class NameIssue { Empty, Duplicate, Limit }

object ValidateHabit {
    operator fun invoke(name: String, id: String?, habits: List<Habit>): NameIssue? = when {
        name.trim().isEmpty() || name.trim().length > 40 -> NameIssue.Empty
        habits.any { it.id != id && it.name.equals(name.trim(), ignoreCase = true) } -> NameIssue.Duplicate
        id == null && habits.size >= 30 -> NameIssue.Limit
        else -> null
    }
}
