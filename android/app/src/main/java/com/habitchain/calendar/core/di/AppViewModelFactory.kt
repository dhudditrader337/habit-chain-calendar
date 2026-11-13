package com.habitchain.calendar.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.time.LocalDateProvider
import com.habitchain.calendar.presentation.about.AboutViewModel
import com.habitchain.calendar.presentation.addhabit.AddHabitViewModel
import com.habitchain.calendar.presentation.app.AppViewModel
import com.habitchain.calendar.presentation.calendar.CalendarViewModel
import com.habitchain.calendar.presentation.game.GameViewModel
import com.habitchain.calendar.presentation.gameover.GameOverViewModel
import com.habitchain.calendar.presentation.habitdetail.HabitDetailViewModel
import com.habitchain.calendar.presentation.menu.MenuViewModel
import com.habitchain.calendar.presentation.settings.SettingsViewModel
import com.habitchain.calendar.presentation.splash.SplashViewModel
import java.time.LocalDate

class AppViewModelFactory(
    private val repository: HabitRepository,
    private val clock: LocalDateProvider,
    private val sample: (LocalDate) -> AppSnapshot,
    private val results: SessionResults,
    private val allowed: () -> Boolean,
    private val version: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AppViewModel::class.java -> AppViewModel(repository, clock)
        SplashViewModel::class.java -> SplashViewModel()
        MenuViewModel::class.java -> MenuViewModel(repository, sample, clock)
        GameViewModel::class.java -> GameViewModel(repository, clock, sample, results)
        GameOverViewModel::class.java -> GameOverViewModel(results, repository)
        AddHabitViewModel::class.java -> AddHabitViewModel(repository, clock, allowed)
        CalendarViewModel::class.java -> CalendarViewModel(repository)
        HabitDetailViewModel::class.java -> HabitDetailViewModel(repository)
        SettingsViewModel::class.java -> SettingsViewModel(repository, allowed, results)
        AboutViewModel::class.java -> AboutViewModel(repository, version)
        else -> error(modelClass.name)
    } as T
}
