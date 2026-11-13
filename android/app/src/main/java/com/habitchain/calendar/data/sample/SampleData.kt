package com.habitchain.calendar.data.sample

import com.habitchain.calendar.domain.model.*
import java.time.LocalDate

object SampleData {
    fun snapshot(today: LocalDate, names: List<String>, notes: List<String>): AppSnapshot {
        val habits = listOf(
            Habit("sample_read", names[0], HabitColor.Leaf, today.minusDays(14)),
            Habit("sample_spanish", names[1], HabitColor.Sky, today.minusDays(14)),
            Habit("sample_stretch", names[2], HabitColor.Forest, today.minusDays(14))
        )
        val offsets = listOf(listOf(-6, -5, -4, -3, -2, -1), listOf(-8, -7, -4, -3, -2, -1), listOf(-3, -2, -1, 0))
        return AppSnapshot(habits, habits.mapIndexed { index, habit ->
            habit.id to HabitHistory(offsets[index].map { today.plusDays(it.toLong()) }.toSet(), mapOf(today.plusDays(offsets[index].last().toLong()) to notes[index]))
        }.toMap())
    }
}
