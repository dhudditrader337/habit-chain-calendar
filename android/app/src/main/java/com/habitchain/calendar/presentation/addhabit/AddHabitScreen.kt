package com.habitchain.calendar.presentation.addhabit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.domain.model.HabitColor
import com.habitchain.calendar.domain.usecase.NameIssue
import com.habitchain.calendar.presentation.dialog.*

@Composable
fun AddHabitRoute(token: String, habitId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val model: AddHabitViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val permission = rememberNotificationRequest(model::refreshPermission)
    val settings = rememberNotificationSettings()
    val haptics = LocalHapticFeedback.current
    val saved by rememberUpdatedState(onSaved)
    val back by rememberUpdatedState(onBack)
    DisposableEffect(token) { model.bind(token, habitId); onDispose {} }
    RouteLifecycle(model::refreshPermission)
    BackHandler { if (state.timeDraft != null) model.closeTime() else if (state.discardOpen) model.dismissDiscard() else model.back() }
    LaunchedEffect(Unit) {
        model.events.collect { event -> when (event) {
            AddHabitEvent.Saved -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); saved() }
            AddHabitEvent.Back -> back()
            AddHabitEvent.RequestPermission -> permission()
            AddHabitEvent.OpenNotificationSettings -> settings()
        } }
    }
    AddHabitScreen(state, model::back, model::name, model::color, model::reminder, model::defaultTime, model::openTime, model::notificationSettings, model::save)
    state.timeDraft?.let { LargeTimePicker(it, model::stepTime, model::confirmTime, model::closeTime) }
    if (state.discardOpen) ConfirmActionDialog(R.string.discard_title, stringResource(R.string.discard_body), R.string.discard, model::discard, model::dismissDiscard, R.string.keep_editing)
}

@Composable
fun AddHabitScreen(state: AddHabitUiState, onBack: () -> Unit, onName: (String) -> Unit, onColor: (HabitColor) -> Unit, onReminder: (Boolean) -> Unit, onDefault: () -> Unit, onCustom: () -> Unit, onNotifications: () -> Unit, onSave: () -> Unit) {
    val nameError = when (state.nameIssue) { NameIssue.Empty -> R.string.name_empty; NameIssue.Duplicate -> R.string.name_duplicate; NameIssue.Limit -> R.string.habit_limit; null -> null }
    AppBackground {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ScreenHeader(stringResource(if (state.editingId == null) R.string.add_title else R.string.edit_title), onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.form_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Image(painterResource(R.drawable.logo), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
            }
            PaperCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(state.name, onName, Modifier.fillMaxWidth(), enabled = !state.saving, label = { Text(stringResource(R.string.habit_name)) },
                    placeholder = { Text(stringResource(R.string.name_placeholder)) }, maxLines = 2, isError = nameError != null,
                    supportingText = { Text(if (nameError != null) stringResource(nameError) else stringResource(R.string.character_count, state.name.length, 40)) })
                Text(stringResource(R.string.color_title), style = MaterialTheme.typography.titleMedium)
                HabitColor.entries.chunked(2).forEach { colors ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { color ->
                            val selected = state.color == color
                            val description = stringResource(if (selected) R.string.selected else R.string.not_selected)
                            val label = when (color) { HabitColor.Leaf -> R.string.color_leaf; HabitColor.Sky -> R.string.color_sky; HabitColor.Forest -> R.string.color_forest; HabitColor.Ink -> R.string.color_ink }
                            OutlinedButton({ onColor(color) }, Modifier.weight(1f).heightIn(min = 56.dp).semantics { stateDescription = description }, enabled = !state.saving,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, color.tint()), colors = ButtonDefaults.outlinedButtonColors(containerColor = color.tint().copy(alpha = if (selected) 0.18f else 0.06f), contentColor = Ink)) {
                                if (selected) { Text(stringResource(R.string.check_symbol)); Spacer(Modifier.width(6.dp)) }
                                Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            PaperCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.remind_me), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(state.reminder, onReminder, enabled = !state.saving)
                }
                Text(stringResource(R.string.reminder_helper), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                if (state.reminder) {
                    val minutes = state.customMinutes ?: state.defaultMinutes
                    Text(stringResource(if (state.customMinutes == null) R.string.reminder_default else R.string.reminder_custom, stringResource(R.string.time_value, minutes / 60, minutes % 60)))
                    SecondaryButton(R.string.default_time, onDefault, Modifier.fillMaxWidth(), enabled = !state.saving)
                    SecondaryButton(R.string.custom_time, onCustom, Modifier.fillMaxWidth(), enabled = !state.saving)
                    if (!state.notificationsAllowed) { InlineStatus(R.string.notifications_off); SecondaryButton(R.string.enable_notifications, onNotifications, Modifier.fillMaxWidth()) }
                }
            }
            if (state.error) InlineStatus(R.string.save_failed, true)
            BottomActionPanel(R.string.form_hint) {
                PrimaryButton(if (state.saving) R.string.saving else if (state.error) R.string.retry else if (state.editingId == null) R.string.create else R.string.save,
                    onSave, enabled = state.name.trim().isNotEmpty(), loading = state.saving)
            }
        }
    }
}
