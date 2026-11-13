package com.habitchain.calendar.presentation.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.domain.model.SessionMode

@Composable
fun GameRoute(id: String, active: Boolean, onBack: () -> Unit, onFinished: (String) -> Unit, onStartToday: () -> Unit, onMenu: () -> Unit) {
    val model: GameViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val finished by rememberUpdatedState(onFinished)
    val isCurrent by rememberUpdatedState(active)
    DisposableEffect(id) { model.bind(id); onDispose { model.pause() } }
    DisposableEffect(active) { if (active) model.resume() else model.pause(); onDispose { model.pause() } }
    RouteLifecycle({ if (isCurrent) model.resume() }, model::pause)
    LaunchedEffect(Unit) {
        model.events.collect { event ->
            when (event) {
                GameEvent.Confirmed -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                is GameEvent.Finished -> if (isCurrent) finished(event.sessionId)
            }
        }
    }
    GameScreen(state, { model.pause(); onBack() }, model::mark, model::skip, model::finish, model::select, model::picker, onStartToday, onMenu)
}

@Composable
fun GameScreen(state: GameUiState, onBack: () -> Unit, onMark: () -> Unit, onSkip: () -> Unit, onFinish: () -> Unit, onSelect: (Int) -> Unit, onPicker: (Boolean) -> Unit, onStartToday: () -> Unit, onMenu: () -> Unit) {
    val session = state.session
    val habit = session?.habits?.getOrNull(state.index)
    val marked = session != null && habit != null && session.date in (session.histories[habit.id]?.completed ?: emptySet())
    val stateLabel = stringResource(if (marked) R.string.marked else R.string.not_marked)
    AppBackground(BackgroundVariant.CheckIn) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val compact = maxHeight < 600.dp
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.then(if (compact) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState())), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp)) {
                    Image(painterResource(R.drawable.icon_pause), null, Modifier.size(20.dp), contentScale = ContentScale.Fit)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.back))
                }
                Text(stringResource(R.string.daily_check_in), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
            }
            if (session == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (session?.mode == SessionMode.Sample) PaperCard {
                Text(stringResource(R.string.sample_session), style = MaterialTheme.typography.labelSmall, color = Forest)
                Text(stringResource(R.string.sample_explanation), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            if (habit != null) {
                PaperCard(Modifier.fillMaxWidth().semantics { stateDescription = stateLabel }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.position, state.index + 1, session.habits.size), style = MaterialTheme.typography.labelSmall, color = Forest)
                        SymbolArt(state.index)
                    }
                    Text(stringResource(R.string.user_text, habit.name), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.date_text, session.date.displayDate()), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                    Text(stringResource(R.string.number, session.date.dayOfMonth), style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold), color = Forest)
                    ChainStrip(session.date, session.histories[habit.id]?.completed ?: emptySet(), habit.color.tint())
                    Text(stringResource(if (marked) R.string.already_marked else R.string.not_yet_marked), color = if (marked) Forest else MutedInk,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (session.habits.size > 1) {
                        TextButton({ onPicker(true) }, enabled = !state.saving) { Text(stringResource(R.string.choose_habit)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton({ onSelect(state.index - 1) }, enabled = !state.saving && state.index > 0) { Text(stringResource(R.string.previous)) }
                            TextButton({ onSelect(state.index + 1) }, enabled = !state.saving && state.index < session.habits.lastIndex) { Text(stringResource(R.string.next)) }
                        }
                    }
                }
            }
            if (state.error) InlineStatus(R.string.mark_failed, true)
            if (state.newDay) { InlineStatus(R.string.new_day); PrimaryButton(R.string.start_today, onStartToday) }
            }
            if (state.ended) {
                InlineStatus(R.string.review_finished)
                HomeButton(onMenu, Modifier.fillMaxWidth())
            } else BottomActionPanel(R.string.check_in_hint) {
                PrimaryButton(if (state.saving) R.string.saving else if (state.error) R.string.retry else R.string.mark_today, onMark,
                    enabled = habit != null && !marked && !state.newDay, loading = state.saving)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onSkip, enabled = !state.saving && !state.newDay && habit != null) { Text(stringResource(R.string.skip_today)) }
                    TextButton(onFinish, enabled = !state.saving && session != null) { Text(stringResource(R.string.finish)) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        }
    }
    if (state.pickerOpen && session != null) AlertDialog(onDismissRequest = { onPicker(false) }, title = { Text(stringResource(R.string.choose_habit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                session.habits.forEachIndexed { index, item ->
                    TextButton({ onSelect(index) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.user_text, item.name)) }
                }
            }
        }, confirmButton = { TextButton({ onPicker(false) }) { Text(stringResource(R.string.close)) } })
}
