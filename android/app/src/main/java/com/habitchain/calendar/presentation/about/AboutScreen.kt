package com.habitchain.calendar.presentation.about

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*

@Composable
fun AboutRoute(privacy: Boolean, onBack: () -> Unit, onMenu: () -> Unit) {
    val model: AboutViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.saved)
    DisposableEffect(privacy) { model.bind(privacy); onDispose {} }
    LaunchedEffect(Unit) { model.events.collect { event -> when (event) {
        AboutEvent.Saved -> snackbar.showSnackbar(saved)
        is AboutEvent.Copy -> {
            val success = runCatching {
                val message = context.getString(R.string.feedback_copy_format, event.message, context.getString(R.string.app_name), model.uiState.value.version, context.packageName)
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), message))
            }.isSuccess
            model.copied(success)
        }
    } } }
    AboutScreen(state, onBack, onMenu, model::privacy, model::contact, snackbar)
    if (state.contactOpen) AlertDialog(onDismissRequest = model::closeContact, title = { Text(stringResource(R.string.feedback_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feedback_helper), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(state.draft, model::draft, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.feedback_label)) }, minLines = 3, enabled = !state.saving,
                    supportingText = { Text(stringResource(R.string.character_count, state.draft.length, 500)) })
                if (state.copied) InlineStatus(R.string.feedback_copied)
                if (state.copyError) InlineStatus(R.string.feedback_copy_failed, true)
                if (state.error) InlineStatus(R.string.save_failed, true)
                SecondaryButton(R.string.copy_message, model::copy, Modifier.fillMaxWidth(), enabled = !state.saving)
            }
        }, confirmButton = { TextButton(model::save, enabled = !state.saving) { Text(stringResource(if (state.saving) R.string.saving else R.string.save_draft)) } },
        dismissButton = { TextButton(model::closeContact, enabled = !state.saving) { Text(stringResource(R.string.close)) } })
}

@Composable
fun AboutScreen(state: AboutUiState, onBack: () -> Unit, onMenu: () -> Unit, onPrivacy: () -> Unit, onContact: () -> Unit, snackbar: SnackbarHostState) {
    AppBackground {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ScreenHeader(stringResource(R.string.about_title), onBack)
            Image(painterResource(R.drawable.logo), null, Modifier.size(96.dp), contentScale = ContentScale.Fit)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(stringResource(R.string.about_category), color = MutedInk, textAlign = TextAlign.Center)
            Text(stringResource(R.string.version, state.version), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            Text(stringResource(R.string.package_name), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            PaperCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.local_data), color = MutedInk)
                TextButton(onPrivacy, Modifier.fillMaxWidth()) { Text(stringResource(if (state.privacyExpanded) R.string.close else R.string.privacy_policy_action)) }
                if (state.privacyExpanded) Text(stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodyLarge)
            }
            PaperCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feedback_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.feedback_helper), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                SecondaryButton(R.string.contact, onContact, Modifier.fillMaxWidth())
            }
            HomeButton(onMenu, Modifier.fillMaxWidth())
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}
