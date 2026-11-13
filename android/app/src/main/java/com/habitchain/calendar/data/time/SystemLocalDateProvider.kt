package com.habitchain.calendar.data.time

import com.habitchain.calendar.domain.time.LocalDateProvider
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class SystemLocalDateProvider : LocalDateProvider {
    override fun today(): LocalDate = LocalDate.now()
    override fun millisUntilTomorrow(): Long {
        val now = ZonedDateTime.now()
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis().coerceAtLeast(1000L)
    }
}
