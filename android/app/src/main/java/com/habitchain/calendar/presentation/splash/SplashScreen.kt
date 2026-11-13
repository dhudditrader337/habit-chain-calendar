package com.habitchain.calendar.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitchain.calendar.R
import com.habitchain.calendar.core.di.ServiceLocator
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.core.ui.*

@Composable
fun SplashRoute(onFinished: () -> Unit) {
    val model: SplashViewModel = viewModel(factory = ServiceLocator.factory)
    val finished by model.finished.collectAsStateWithLifecycle()
    val state by model.uiState.collectAsStateWithLifecycle()
    val currentFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(finished) { if (finished && model.takeCompletion()) currentFinished() }
    SplashScreen(state)
}

@Composable
fun SplashScreen(state: SplashUiState) {
    val loop = rememberInfiniteTransition(label = "chain float")
    val drift by loop.animateFloat(-5f, 5f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "drift")
    val rotation by loop.animateFloat(-2f, 2f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "tilt")
    val glow by loop.animateFloat(0.12f, 0.25f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "glow")
    val loading = stringResource(R.string.loading_app)
    val entry = remember { MutableTransitionState(false).apply { targetState = true } }
    AppBackground(BackgroundVariant.Splash) {
        Surface(Modifier.align(Alignment.TopEnd).offset(50.dp, 120.dp).size(180.dp, 95.dp).graphicsLayer { rotationZ = -35f }, shape = RoundedCornerShape(60.dp),
            color = PaperWhite.copy(alpha = 0f), border = BorderStroke(12.dp, SoftGreen.copy(alpha = glow))) {}
        Surface(Modifier.align(Alignment.BottomStart).offset((-70).dp, (-140).dp).size(200.dp, 105.dp).graphicsLayer { rotationZ = -35f }, shape = RoundedCornerShape(60.dp),
            color = PaperWhite.copy(alpha = 0f), border = BorderStroke(12.dp, SoftGreen.copy(alpha = glow))) {}
        AnimatedVisibility(entry, enter = fadeIn(tween(400)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 300f), initialScale = 0.96f), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.splash_label), style = MaterialTheme.typography.labelSmall, color = SoftGreen)
            Spacer(Modifier.height(28.dp))
            Image(painterResource(R.drawable.logo), stringResource(R.string.logo_description), Modifier.size(216.dp).graphicsLayer { translationY = drift.dp.toPx(); rotationZ = rotation }, contentScale = ContentScale.Fit)
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, color = PaperWhite)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = PaperWhite)
            Spacer(Modifier.height(36.dp))
            Box(Modifier.width(160.dp).height(4.dp)) {
                if (!state.isReady) LinearProgressIndicator(modifier = Modifier.fillMaxSize().semantics { contentDescription = loading }, color = Accent, trackColor = SoftGreen.copy(alpha = 0.4f))
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}
