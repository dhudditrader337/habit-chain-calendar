package com.habitchain.calendar.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.habitchain.calendar.core.theme.Forest

@Composable
fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "confirmed mark")
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.5f)
            lineTo(size.width * 0.42f, size.height * 0.77f)
            lineTo(size.width * 0.86f, size.height * 0.22f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * progress, segment, true)
        drawPath(segment, Forest, style = Stroke(width = size.width * 0.12f, cap = StrokeCap.Round))
    }
}
