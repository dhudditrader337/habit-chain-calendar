package com.habitchain.calendar.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.presentation.dialog.*

@Composable
fun SettingsRoute(onBack: () -> Unit, onMenu: () -> Unit, onPrivacy: () -> Unit) {
    val model: SettingsViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val permission = rememberNotificationRequest(model::refresh)
    val notificationSettings = rememberNotificationSettings()
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.settings_saved)
    val menu by rememberUpdatedState(onMenu)
    RouteLifecycle(model::refresh)
    LaunchedEffect(Unit) { model.events.collect { event -> when (event) {
        SettingsEvent.Saved -> snackbar.showSnackbar(saved)
        SettingsEvent.Reset -> menu()
        SettingsEvent.RequestPermission -> permission()
        SettingsEvent.NotificationSettings -> notificationSettings()
    } } }
    SettingsScreen(state, onBack, onPrivacy, model::reminders, model::openTime, model::manage, { model.resetDialog(1) }, model::retry, snackbar)
    state.timeDraft?.let { LargeTimePicker(it, model::stepTime, model::confirmTime, model::closeTime) }
    if (state.resetStep > 0) ConfirmActionDialog(
        if (state.resetStep == 1) R.string.reset_title else R.string.reset_final_title,
        stringResource(if (state.resetStep == 1) R.string.reset_body else R.string.reset_final_body),
        if (state.resetStep == 1) R.string.confirm else R.string.delete_all_data,
        { if (state.resetStep == 1) model.resetDialog(2) else model.reset() }, { model.resetDialog(0) }, R.string.keep_data, state.saving)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onBack: () -> Unit, onPrivacy: () -> Unit, onReminders: (Boolean) -> Unit, onTime: () -> Unit, onManage: () -> Unit, onReset: () -> Unit, onRetry: () -> Unit, snackbar: SnackbarHostState) {
    AppBackground {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ScreenHeader(stringResource(R.string.settings_title), onBack)
            PaperCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.habit_reminders), modifier = Modifier.weight(1f))
                    Switch(state.settings.remindersEnabled, onReminders, enabled = !state.saving)
                }
                HorizontalDivider(color = SoftGreen)
                Text(stringResource(R.string.default_reminder_time), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                Text(stringResource(R.string.time_value, state.settings.defaultReminderMinutes / 60, state.settings.defaultReminderMinutes % 60), style = MaterialTheme.typography.headlineMedium, color = Forest)
                Text(stringResource(R.string.default_time_helper), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                SecondaryButton(R.string.change_time, onTime, Modifier.fillMaxWidth(), enabled = !state.saving)
                Text(stringResource(R.string.notification_status, stringResource(if (state.notificationsAllowed) R.string.allowed else R.string.not_allowed)), style = MaterialTheme.typography.bodyMedium)
                SecondaryButton(R.string.manage_notifications, onManage, Modifier.fillMaxWidth())
                Text(stringResource(R.string.reminder_delivery), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            PaperCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.device_data), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.local_data), color = MutedInk)
                SecondaryButton(R.string.reset_all_data, onReset, Modifier.fillMaxWidth(), enabled = !state.saving, destructive = true)
                TextButton(onPrivacy, Modifier.fillMaxWidth()) { Text(stringResource(R.string.privacy_policy_action)) }
            }
            if (state.error) { InlineStatus(R.string.save_failed, true); SecondaryButton(R.string.retry, onRetry) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}
