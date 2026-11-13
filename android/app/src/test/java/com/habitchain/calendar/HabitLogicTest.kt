package com.habitchain.calendar

import com.habitchain.calendar.domain.model.Habit
import com.habitchain.calendar.domain.model.HabitColor
import com.habitchain.calendar.domain.usecase.CalculateChains
import com.habitchain.calendar.domain.usecase.Chains
import com.habitchain.calendar.domain.usecase.NameIssue
import com.habitchain.calendar.domain.usecase.ValidateHabit
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HabitLogicTest {
    private val today = LocalDate.of(2026, 11, 13)
    private fun days(vararg offsets: Long) = offsets.map { today.plusDays(it) }.toSet()

    @Test fun emptyHistoryHasNoChain() { assertEquals(Chains(0, 0, 0), CalculateChains(emptySet(), today)) }
    @Test fun yesterdayKeepsTheChainActive() { assertEquals(Chains(3, 3, 3), CalculateChains(days(-3, -2, -1), today)) }
    @Test fun markingTodayExtendsTheChain() { assertEquals(Chains(4, 4, 4), CalculateChains(days(-3, -2, -1, 0), today)) }
    @Test fun aGapBreaksTheCurrentChain() { assertEquals(Chains(0, 2, 2), CalculateChains(days(-3, -2), today)) }
    @Test fun duplicateDatesCannotIncreaseTheChain() {
        val dates = days(-1, 0) + today + today
        assertEquals(Chains(2, 2, 2), CalculateChains(dates, today))
    }
    @Test fun removingAMiddleDaySplitsTheChain() { assertEquals(Chains(2, 2, 3), CalculateChains(days(-3, -2, -1, 0) - today.minusDays(2), today)) }
    @Test fun longestRunDoesNotBridgeMissingDays() { assertEquals(Chains(1, 1, 2), CalculateChains(days(-3, -1), today)) }
    @Test fun leapDayCountsAsOneCivilDay() {
        val date = LocalDate.of(2024, 3, 1)
        assertEquals(Chains(3, 3, 3), CalculateChains(setOf(date, date.minusDays(1), date.minusDays(2)), date))
    }
    @Test fun yearBoundariesDoNotBreakChains() {
        val date = LocalDate.of(2027, 1, 1)
        assertEquals(Chains(3, 3, 3), CalculateChains(setOf(date, date.minusDays(1), date.minusDays(2)), date))
    }
    @Test fun midnightChangesTheChainAnchor() {
        assertEquals(1, CalculateChains(days(0), today.plusDays(1)).current)
        assertEquals(0, CalculateChains(days(0), today.plusDays(2)).current)
    }
    @Test fun movingClockBackPreservesHistoricalRecords() { assertEquals(Chains(0, 2, 2), CalculateChains(days(1, 2), today)) }
    @Test fun namesAreTrimmedAndDuplicatesAreCaseInsensitive() {
        val habit = Habit("read", "Read ten pages", HabitColor.Leaf, today)
        assertEquals(NameIssue.Empty, ValidateHabit("   ", null, listOf(habit)))
        assertEquals(NameIssue.Duplicate, ValidateHabit("  READ TEN PAGES ", null, listOf(habit)))
        assertNull(ValidateHabit("Read ten pages", habit.id, listOf(habit)))
    }
    @Test fun newHabitLimitDoesNotPreventEditing() {
        val habits = (1..30).map { Habit(it.toString(), "Daily practice $it", HabitColor.Leaf, today) }
        assertEquals(NameIssue.Limit, ValidateHabit("Walk outdoors", null, habits))
        assertNull(ValidateHabit("Walk outdoors", "1", habits))
    }
}
