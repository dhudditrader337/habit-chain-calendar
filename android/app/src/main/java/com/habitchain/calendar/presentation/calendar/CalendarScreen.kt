package com.habitchain.calendar.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.presentation.dialog.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarRoute(id: String, onBack: () -> Unit, onMenu: () -> Unit, onDetails: () -> Unit) {
    val model: CalendarViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val removed = stringResource(R.string.mark_removed)
    val undo = stringResource(R.string.undo)
    DisposableEffect(id) { model.bind(id); onDispose {} }
    LaunchedEffect(Unit) { model.events.collect { event ->
        when (event) {
            CalendarEvent.Saved -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            CalendarEvent.Removed -> if (snackbar.showSnackbar(removed, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) model.undo()
        }
    } }
    CalendarScreen(state, onBack, onMenu, onDetails, model::month, model::today, model::select, model::markAction, model::openNote, model::retry, snackbar)
    if (state.confirmRemove) ConfirmActionDialog(R.string.remove_title, stringResource(R.string.remove_body), R.string.remove_mark, model::remove, model::dismissRemove, saving = state.saving)
    state.noteDraft?.let { DayNoteDialog(state.selected.displayDate(), it, state.saving, state.error, model::note, model::saveNote, model::closeNote) }
}

@Composable
fun CalendarScreen(state: CalendarUiState, onBack: () -> Unit, onMenu: () -> Unit, onDetails: () -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit, onSelect: (LocalDate) -> Unit, onMark: () -> Unit, onNote: () -> Unit, onRetry: () -> Unit, snackbar: SnackbarHostState) {
    AppBackground {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(state.habit?.name ?: stringResource(R.string.calendar), onBack)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.habit == null && !state.loading) { InlineStatus(R.string.unavailable_habit); HomeButton(onMenu) }
            if (state.habit != null) {
                TextButton(onDetails) { Text(stringResource(R.string.habit_details)) }
                PaperCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    Text(stringResource(R.string.date_text, state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp))
                    Column(Modifier.fillMaxWidth()) {
                        TextButton({ onMonth(-1) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.previous_month)) }
                        TextButton({ onMonth(1) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.next_month)) }
                    }
                    if (state.month != YearMonth.from(state.today)) TextButton(onToday) { Text(stringResource(R.string.today)) }
                    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        AnimatedContent(state.month, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "month") { month ->
                            MonthGrid(month, state, onSelect)
                        }
                    }
                    if (state.history.completed.none { YearMonth.from(it) == state.month }) Text(stringResource(R.string.empty_month), style = MaterialTheme.typography.bodyMedium, color = MutedInk, modifier = Modifier.padding(16.dp))
                }
                if (state.history.completed.any { it.isAfter(state.today) }) InlineStatus(R.string.clock_change)
                PaperCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.date_text, state.selected.displayDate()), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (state.marked) R.string.marked else R.string.not_marked), color = Forest, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    state.history.notes[state.selected]?.let { Text(stringResource(R.string.user_text, it)) }
                    if (state.eligible) SecondaryButton(if (state.history.notes.containsKey(state.selected)) R.string.edit_note else R.string.add_note, onNote, Modifier.fillMaxWidth(), enabled = !state.saving)
                }
                if (state.error) { InlineStatus(R.string.save_failed, true); SecondaryButton(R.string.retry, onRetry) }
                BottomActionPanel(R.string.calendar_hint) {
                    PrimaryButton(if (state.saving) R.string.saving else if (state.marked) R.string.remove_mark else R.string.mark_day, onMark, enabled = state.eligible, loading = state.saving)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}

@Composable
private fun MonthGrid(month: YearMonth, state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val weekdays = listOf(R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed, R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun)
    val descriptions = listOf(R.string.monday, R.string.tuesday, R.string.wednesday, R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
    val cellSize = maxOf(48f, 32f * LocalDensity.current.fontScale).dp
    Column(Modifier.width(cellSize * 7)) {
        Row { weekdays.forEachIndexed { index, label ->
            val description = stringResource(descriptions[index])
            Box(Modifier.size(cellSize).semantics { contentDescription = description }, contentAlignment = Alignment.Center) { Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MutedInk) }
        } }
        val offset = month.atDay(1).dayOfWeek.value - 1
        val rows = (offset + month.lengthOfMonth() + 6) / 7
        repeat(rows) { row ->
            Box {
            Canvas(Modifier.width(cellSize * 7).height(cellSize)) {
                repeat(6) { column ->
                    val number = row * 7 + column - offset + 1
                    if (number in 1 until month.lengthOfMonth() && month.atDay(number) in state.history.completed && month.atDay(number + 1) in state.history.completed) {
                        drawLine(state.habit?.color?.tint() ?: Primary, Offset((column + 0.5f) * cellSize.toPx(), size.height / 2), Offset((column + 1.5f) * cellSize.toPx(), size.height / 2), 5.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            Row {
                repeat(7) { column ->
                    val number = row * 7 + column - offset + 1
                    if (number in 1..month.lengthOfMonth()) {
                        val day = month.atDay(number)
                        val marked = day in state.history.completed
                        val selected = day == state.selected
                        val enabled = state.habit?.let { !day.isBefore(it.createdOn) && !day.isAfter(state.today) } == true && !state.saving
                        CalendarDayCell(day, marked, selected, day == state.today, enabled, cellSize, onSelect)
                    } else Spacer(Modifier.size(cellSize))
                }
            }
            }
        }
    }
}
