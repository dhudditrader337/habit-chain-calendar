package com.habitchain.calendar.core.navigation

sealed interface Screen {
    data object Splash : Screen
    data object Menu : Screen
    data class Game(val sessionId: String) : Screen
    data class GameOver(val sessionId: String) : Screen
    data class AddHabit(val habitId: String? = null, val draftId: String = java.util.UUID.randomUUID().toString()) : Screen
    data class Calendar(val habitId: String) : Screen
    data class HabitDetail(val habitId: String) : Screen
    data object Settings : Screen
    data class About(val privacy: Boolean = false) : Screen
}
