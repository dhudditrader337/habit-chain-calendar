package com.habitchain.calendar.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.core.navigation.Screen
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.time.LocalDateProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(private val repository: HabitRepository, private val clock: LocalDateProvider) : ViewModel() {
    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen = _screen.asStateFlow()
    private var backStack: List<Screen> = emptyList()
    private var midnightJob: Job? = null

    init { viewModelScope.launch { repository.load() } }

    fun finishSplash() {
        if (_screen.value == Screen.Splash) _screen.update { Screen.Menu }
    }

    fun navigate(destination: Screen) {
        if (_screen.value == destination) return
        if (_screen.value != Screen.Splash) backStack = backStack + _screen.value
        _screen.update { destination }
    }

    fun play() = navigate(Screen.Game(UUID.randomUUID().toString()))

    fun playAgain() {
        backStack = backStack.filterNot { it is Screen.Game || it is Screen.GameOver }
        if (backStack.isEmpty()) backStack = listOf(Screen.Menu)
        _screen.update { Screen.Game(UUID.randomUUID().toString()) }
    }

    fun menu() { backStack = emptyList(); _screen.update { Screen.Menu } }

    fun back() {
        val previous = backStack.lastOrNull() ?: Screen.Menu
        backStack = backStack.dropLast(1)
        _screen.update { previous }
    }

    fun resume() {
        midnightJob?.cancel()
        midnightJob = viewModelScope.launch {
            repository.refreshDate()
            while (isActive) {
                delay(clock.millisUntilTomorrow())
                repository.refreshDate()
            }
        }
    }

    fun pause() { midnightJob?.cancel(); midnightJob = null }
}
