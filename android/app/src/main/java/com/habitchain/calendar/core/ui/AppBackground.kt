package com.habitchain.calendar.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.habitchain.calendar.R
import com.habitchain.calendar.core.theme.*

enum class BackgroundVariant { Splash, Home, CheckIn, Result, Paper }

@Composable
fun AppBackground(variant: BackgroundVariant = BackgroundVariant.Paper, content: @Composable BoxScope.() -> Unit) {
    val colors = when (variant) {
        BackgroundVariant.Splash -> listOf(Forest, Forest.copy(alpha = 0.94f), Primary.copy(alpha = 0.9f))
        BackgroundVariant.Home -> listOf(PaperGreen.copy(alpha = 0.97f), PaperWhite.copy(alpha = 0.94f), PaperGreen.copy(alpha = 0.93f))
        BackgroundVariant.CheckIn -> listOf(PaperGreen.copy(alpha = 0.96f), SoftGreen.copy(alpha = 0.85f), PaperGreen.copy(alpha = 0.94f))
        BackgroundVariant.Result -> listOf(PaperWhite.copy(alpha = 0.97f), PaperGreen.copy(alpha = 0.92f), SoftGreen.copy(alpha = 0.91f))
        BackgroundVariant.Paper -> listOf(PaperWhite.copy(alpha = 0.96f), PaperGreen.copy(alpha = 0.95f))
    }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_game), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
        content()
    }
}
