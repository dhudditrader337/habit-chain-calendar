package com.habitchain.calendar.domain.usecase

import java.time.LocalDate

data class Chains(val current: Int, val longest: Int, val total: Int)

object CalculateChains {
    operator fun invoke(dates: Set<LocalDate>, today: LocalDate): Chains {
        val eligible = dates.filter { !it.isAfter(today) }.toSet()
        var anchor = if (today in eligible) today else today.minusDays(1)
        var current = 0
        while (anchor in eligible) { current++; anchor = anchor.minusDays(1) }
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        dates.sorted().forEach { date ->
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }
        return Chains(current, longest, dates.size)
    }
}
