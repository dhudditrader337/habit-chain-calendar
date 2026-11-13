package com.habitchain.calendar.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.core.theme.*

@Composable
fun PrimaryButton(labelRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.85f, stiffness = 500f), label = "press")
    val shape = RoundedCornerShape(16.dp)
    val gradient = when {
        loading -> listOf(SoftGreen, PaperGreen)
        enabled -> listOf(ActionStrong, ActionDeep)
        else -> listOf(NordicMist, NordicMist)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled && !loading) 6.dp else 0.dp, shape)
            .background(Brush.verticalGradient(gradient), shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = if (loading) Ink else MutedInk),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), color = Forest, strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)) }
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(labelRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (destructive) Error else SoftGreen),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = PaperWhite.copy(alpha = 0.8f), contentColor = if (destructive) Error else ActionStrong)
    ) { Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge) }
}
