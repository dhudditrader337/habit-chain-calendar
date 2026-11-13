package com.habitchain.calendar.presentation.gameover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*
import com.habitchain.calendar.domain.model.SessionMode

@Composable
fun GameOverRoute(id: String, onAgain: () -> Unit, onMenu: () -> Unit, onCreate: () -> Unit) {
    val model: GameOverViewModel = viewModel(factory = ServiceLocator.factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    DisposableEffect(id) { model.bind(id); onDispose {} }
    GameOverScreen(state, onAgain, onMenu, onCreate)
}

@Composable
fun GameOverScreen(state: GameOverUiState, onAgain: () -> Unit, onMenu: () -> Unit, onCreate: () -> Unit) {
    val result = state.result
    val title = when {
        result == null || result.changedNames.isEmpty() -> R.string.result_no_changes
        result.markedCount == result.totalCount -> R.string.result_complete
        else -> R.string.result_partial
    }
    AppBackground(BackgroundVariant.Result) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(if (result?.mode == SessionMode.Sample) R.string.sample_session else R.string.result_eyebrow), style = MaterialTheme.typography.labelSmall, color = Forest)
            Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.sprite_4), stringResource(R.string.result_art), Modifier.size(132.dp), contentScale = ContentScale.Fit)
                Image(painterResource(R.drawable.sprite_5), null, Modifier.size(48.dp).align(Alignment.BottomEnd), contentScale = ContentScale.Fit)
            }
            Text(stringResource(title), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (result != null) {
                if (result.mode == SessionMode.Sample) Text(stringResource(R.string.sample_result), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                PaperCard(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text(stringResource(R.string.summary_count, result.markedCount, result.totalCount), style = MaterialTheme.typography.titleLarge)
                    if (result.alreadyCount > 0) Text(stringResource(R.string.already_count, result.alreadyCount), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                    if (result.changedNames.isEmpty()) Text(stringResource(R.string.no_new_marks), color = MutedInk)
                    else {
                        Text(stringResource(R.string.changed_title), style = MaterialTheme.typography.labelSmall, color = Forest)
                        result.changedNames.forEach { Text(stringResource(R.string.user_text, it)) }
                    }
                    if (result.pendingNames.isNotEmpty()) {
                        Text(stringResource(R.string.pending_title), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                        result.pendingNames.forEach { Text(stringResource(R.string.user_text, it), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (result.currentChain > 0 && result.longestChain > 0) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(result.currentChain, R.string.current_chain, Modifier.weight(1f))
                    StatCard(result.longestChain, R.string.longest_chain, Modifier.weight(1f))
                }
            }
            BottomActionPanel(R.string.result_hint) {
                PrimaryButton(R.string.play_again, onAgain)
                HomeButton(onMenu, Modifier.fillMaxWidth())
                if (result?.mode == SessionMode.Sample) TextButton(onCreate, Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_habit)) }
            }
        }
    }
}
