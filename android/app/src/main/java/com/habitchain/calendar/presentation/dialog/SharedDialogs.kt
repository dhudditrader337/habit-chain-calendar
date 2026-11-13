package com.habitchain.calendar.presentation.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.R
import com.habitchain.calendar.core.ui.*

@Composable
fun ConfirmActionDialog(titleRes: Int, message: String, confirmRes: Int, onConfirm: () -> Unit, onDismiss: () -> Unit, dismissRes: Int = R.string.cancel, saving: Boolean = false) {
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(titleRes)) }, text = { Text(message) },
        confirmButton = { TextButton(onConfirm, enabled = !saving) { Text(stringResource(confirmRes)) } },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text(stringResource(dismissRes)) } })
}

@Composable
fun LargeTimePicker(minutes: Int, onStep: (Int) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.default_reminder_time)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.time_value, minutes / 60, minutes % 60), style = MaterialTheme.typography.displaySmall)
                SecondaryButton(R.string.hour_up, { onStep(60) }, Modifier.fillMaxWidth())
                SecondaryButton(R.string.hour_down, { onStep(-60) }, Modifier.fillMaxWidth())
                SecondaryButton(R.string.minute_up, { onStep(15) }, Modifier.fillMaxWidth())
                SecondaryButton(R.string.minute_down, { onStep(-15) }, Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton(onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun DayNoteDialog(date: String, value: String, saving: Boolean, error: Boolean, onValue: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(date) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.note_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value, onValue, label = { Text(stringResource(R.string.note_label)) }, minLines = 3, enabled = !saving,
                    supportingText = { Text(stringResource(R.string.character_count, value.length, 280)) })
                if (error) InlineStatus(R.string.save_failed, true)
            }
        }, confirmButton = { TextButton(onSave, enabled = !saving) { Text(stringResource(if (saving) R.string.saving else R.string.save_note)) } },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text(stringResource(R.string.cancel)) } })
}
