package com.habitchain.calendar.presentation.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.navigation.Screen
import com.habitchain.calendar.core.ui.RouteLifecycle
import com.habitchain.calendar.presentation.about.AboutRoute
import com.habitchain.calendar.presentation.addhabit.AddHabitRoute
import com.habitchain.calendar.presentation.calendar.CalendarRoute
import com.habitchain.calendar.presentation.game.GameRoute
import com.habitchain.calendar.presentation.gameover.GameOverRoute
import com.habitchain.calendar.presentation.habitdetail.HabitDetailRoute
import com.habitchain.calendar.presentation.menu.MenuRoute
import com.habitchain.calendar.presentation.settings.SettingsRoute
import com.habitchain.calendar.presentation.splash.SplashRoute

@Composable
fun AppRoot() {
    val app: AppViewModel = viewModel(factory = ServiceLocator.factory)
    val screen by app.screen.collectAsStateWithLifecycle()
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.let { activity ->
            val bars = WindowCompat.getInsetsController(activity.window, view)
            bars.isAppearanceLightStatusBars = screen != Screen.Splash
            bars.isAppearanceLightNavigationBars = screen != Screen.Splash
        }
    }
    RouteLifecycle(app::resume, app::pause)
    BackHandler(enabled = screen != Screen.Menu && screen != Screen.Splash) { app.back() }
    AnimatedContent(targetState = screen, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "navigation") { destination ->
        when (destination) {
            Screen.Splash -> SplashRoute(app::finishSplash)
            Screen.Menu -> MenuRoute(app::play, { app.navigate(Screen.AddHabit()) }, { app.navigate(Screen.Calendar(it)) }, { app.navigate(Screen.HabitDetail(it)) }, { app.navigate(Screen.Settings) }, { app.navigate(Screen.About()) })
            is Screen.Game -> GameRoute(destination.sessionId, destination == screen, app::back, { app.navigate(Screen.GameOver(it)) }, app::playAgain, app::menu)
            is Screen.GameOver -> GameOverRoute(destination.sessionId, app::playAgain, app::menu, { app.navigate(Screen.AddHabit()) })
            is Screen.AddHabit -> AddHabitRoute(destination.draftId, destination.habitId, app::back, { if (destination.habitId == null) app.menu() else app.back() })
            is Screen.Calendar -> CalendarRoute(destination.habitId, app::back, app::menu, { app.navigate(Screen.HabitDetail(destination.habitId)) })
            is Screen.HabitDetail -> HabitDetailRoute(destination.habitId, app::back, app::menu, { app.navigate(Screen.AddHabit(destination.habitId)) }, { app.navigate(Screen.Calendar(destination.habitId)) })
            Screen.Settings -> SettingsRoute(app::back, app::menu, { app.navigate(Screen.About(true)) })
            is Screen.About -> AboutRoute(destination.privacy, app::back, app::menu)
        }
    }
}
