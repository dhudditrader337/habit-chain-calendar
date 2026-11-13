package com.habitchain.calendar.presentation.habitdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.presentation.dialog.*
import java.time.LocalDate

@Composable
fun HabitDetailRoute(id: String, onBack: () -> Unit, onMenu: () -> Unit, onEdit: () -> Unit, onCalendar: () -> Unit) {
    val model: HabitDetailViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val menu by rememberUpdatedState(onMenu)
    DisposableEffect(id) { model.bind(id); onDispose {} }
    LaunchedEffect(Unit) { model.events.collect { event -> if (event == DetailEvent.Deleted) menu() else haptics.performHapticFeedback(HapticFeedbackType.LongPress) } }
    HabitDetailScreen(state, onBack, onMenu, onEdit, onCalendar, model::expandNotes, model::openNote, { model.deleteDialog(true) }, model::retry)
    if (state.deleteOpen) ConfirmActionDialog(R.string.delete_title, stringResource(R.string.delete_body, state.habit?.name.orEmpty()), R.string.delete, model::delete, { model.deleteDialog(false) }, saving = state.saving)
    state.noteDate?.let { DayNoteDialog(it.displayDate(), state.noteDraft, state.saving, state.error, model::note, model::saveNote, model::closeNote) }
}

@Composable
fun HabitDetailScreen(state: HabitDetailUiState, onBack: () -> Unit, onMenu: () -> Unit, onEdit: () -> Unit, onCalendar: () -> Unit, onAllNotes: () -> Unit, onNote: (LocalDate) -> Unit, onDelete: () -> Unit, onRetry: () -> Unit) {
    AppBackground {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(stringResource(R.string.habit_details), onBack)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.habit == null && !state.loading) { InlineStatus(R.string.unavailable_habit); HomeButton(onMenu) }
            state.habit?.let { habit ->
                Text(stringResource(R.string.user_text, habit.name), style = MaterialTheme.typography.headlineMedium)
                TextButton(onEdit) { Text(stringResource(R.string.edit)) }
                if (state.chains.current > 0 && state.chains.longest > 0) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(state.chains.current, R.string.current_chain, Modifier.weight(1f))
                    StatCard(state.chains.longest, R.string.longest_chain, Modifier.weight(1f))
                }
                PaperCard(Modifier.fillMaxWidth()) {
                    if (state.chains.total > 0) {
                        Text(stringResource(R.string.marked_days), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                        Text(stringResource(R.string.number, state.chains.total), style = MaterialTheme.typography.headlineMedium, color = Forest)
                        if (state.chains.current == 0 && state.chains.longest > 0) Text(stringResource(R.string.chain_days, state.chains.longest), style = MaterialTheme.typography.bodyMedium)
                    } else Text(stringResource(R.string.first_day), color = MutedInk)
                    ChainStrip(state.today, state.history.completed, habit.color.tint())
                    PrimaryButton(R.string.open_calendar, onCalendar)
                    if (habit.reminderEnabled) {
                        val minutes = habit.customReminderMinutes ?: state.defaultMinutes
                        Text(stringResource(if (habit.customReminderMinutes == null) R.string.reminder_default else R.string.reminder_custom, stringResource(R.string.time_value, minutes / 60, minutes % 60)), style = MaterialTheme.typography.bodyMedium)
                    } else Text(stringResource(R.string.reminder_off), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                }
                PaperCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.day_notes), style = MaterialTheme.typography.titleLarge)
                    val notes = state.history.notes.toList().sortedByDescending { it.first }
                    if (notes.isEmpty()) {
                        Text(stringResource(R.string.notes_empty), color = MutedInk)
                        SecondaryButton(R.string.add_note, { onNote(state.today) })
                    } else {
                        notes.take(if (state.allNotes) notes.size else 5).forEach { (date, note) ->
                            Text(stringResource(R.string.date_text, date.displayDate()), style = MaterialTheme.typography.labelSmall, color = Forest)
                            Text(stringResource(R.string.user_text, note))
                            if (!date.isAfter(state.today)) TextButton({ onNote(date) }) { Text(stringResource(R.string.edit_note)) }
                            HorizontalDivider(color = SoftGreen)
                        }
                        if (!state.allNotes && notes.size > 5) TextButton(onAllNotes) { Text(stringResource(R.string.see_all_notes)) }
                        SecondaryButton(R.string.add_note, { onNote(state.today) })
                    }
                }
                if (state.error) { InlineStatus(R.string.save_failed, true); SecondaryButton(R.string.retry, onRetry) }
                SecondaryButton(R.string.delete_habit, onDelete, Modifier.fillMaxWidth(), enabled = !state.saving, destructive = true)
            }
        }
    }
}
