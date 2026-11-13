package com.habitchain.calendar.core.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberNotificationRequest(onReturn: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentReturn by rememberUpdatedState(onReturn)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { currentReturn() }
    return {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)); currentReturn() }
    }
}

@Composable
fun rememberNotificationSettings(): () -> Unit {
    val context = LocalContext.current
    return { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
}

@Composable
fun RouteLifecycle(onResume: () -> Unit, onPause: () -> Unit = {}) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resumed by rememberUpdatedState(onResume)
    val paused by rememberUpdatedState(onPause)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed()
            if (event == Lifecycle.Event.ON_PAUSE) paused()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumed()
        onDispose { lifecycle.removeObserver(observer); paused() }
    }
}
