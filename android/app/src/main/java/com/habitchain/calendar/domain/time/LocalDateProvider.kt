package com.habitchain.calendar.domain.time

import java.time.LocalDate

interface LocalDateProvider {
    fun today(): LocalDate
    fun millisUntilTomorrow(): Long
}
