package com.habitchain.calendar.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MenuRoute(onPlay: () -> Unit, onAdd: () -> Unit, onCalendar: (String) -> Unit, onDetails: (String) -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
    val model: MenuViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val removed = stringResource(R.string.mark_removed)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(Unit) {
        model.events.collect { event ->
            when (event) {
                MenuEvent.Confirmed -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                MenuEvent.Removed -> if (snackbar.showSnackbar(removed, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) model.undo()
            }
        }
    }
    MenuScreen(state, onPlay, onAdd, model::toggle, onCalendar, onDetails, onSettings, onAbout, model::retry, snackbar)
}

@Composable
fun MenuScreen(state: MenuUiState, onPlay: () -> Unit, onAdd: () -> Unit, onToggle: (String) -> Unit, onCalendar: (String) -> Unit, onDetails: (String) -> Unit, onSettings: () -> Unit, onAbout: () -> Unit, onRetry: () -> Unit, snackbar: SnackbarHostState) {
    AppBackground(BackgroundVariant.Home) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val compact = maxHeight < 600.dp
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().padding(horizontal = 20.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp)) {
                    item {
                        Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelSmall, color = Forest)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.your_habits), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.date_text, state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                        Row {
                            TextButton(onSettings, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings)) }
                            TextButton(onAbout, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.about)) }
                        }
                    }
                    item {
                        PaperCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.hero_support), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                                }
                                Image(painterResource(R.drawable.logo), null, Modifier.size(92.dp), contentScale = ContentScale.Fit)
                            }
                            if (state.habits.isNotEmpty()) Text(stringResource(R.string.summary_count, state.doneToday, state.habits.size), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                    if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (state.error) item { PaperCard { InlineStatus(R.string.load_failed, true); SecondaryButton(R.string.retry, onRetry) } }
                    if (state.saveError) item { InlineStatus(R.string.save_failed, true) }
                    if (state.doneToday > 0 && state.activeChains > 0) item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(state.doneToday, R.string.done_today, Modifier.weight(1f))
                            StatCard(state.activeChains, R.string.active_chains, Modifier.weight(1f))
                        }
                    }
                    if (state.habits.isEmpty() && !state.loading && !state.error) {
                        item { PaperCard { Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge); Text(stringResource(R.string.empty_body), color = MutedInk); SecondaryButton(R.string.create_habit, onAdd) } }
                        item { Text(stringResource(R.string.examples), style = MaterialTheme.typography.titleLarge); Text(stringResource(R.string.example_hint), style = MaterialTheme.typography.bodyMedium, color = MutedInk) }
                        itemsIndexed(state.examples, key = { _, item -> item.habit.id }) { index, item ->
                            HabitCard(item, index, true, false, onAdd, onAdd, onAdd)
                        }
                    } else itemsIndexed(state.habits, key = { _, item -> item.habit.id }) { index, item ->
                        HabitCard(item, index, false, state.savingId != null, { onToggle(item.habit.id) }, { onCalendar(item.habit.id) }, { onDetails(item.habit.id) })
                    }
                    if (compact) item { MenuActions(onPlay, onAdd, !state.loading && !state.error) }
                }
                if (!compact) { MenuActions(onPlay, onAdd, !state.loading && !state.error); Spacer(Modifier.height(8.dp)) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun MenuActions(onPlay: () -> Unit, onAdd: () -> Unit, enabled: Boolean) {
    BottomActionPanel(R.string.home_hint) {
        PrimaryButton(R.string.play, onPlay, enabled = enabled)
        SecondaryButton(R.string.add_habit, onAdd, Modifier.fillMaxWidth(), enabled = enabled)
    }
}
