package com.habitchain.calendar.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.R
import com.habitchain.calendar.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun LocalDate.displayDate(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
fun ChainStrip(today: LocalDate, completed: Set<LocalDate>, color: Color = Primary) {
    val markedText = stringResource(R.string.marked)
    val unmarkedText = stringResource(R.string.not_marked)
    val cellSize = maxOf(48f, 32f * LocalDensity.current.fontScale).dp
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        (6 downTo 0).forEach { offset ->
            val day = today.minusDays(offset.toLong())
            val marked = day in completed
            if (offset < 6) {
                Surface(Modifier.width(4.dp).height(4.dp), color = if (marked && day.minusDays(1) in completed) color else SoftGreen.copy(alpha = 0.3f)) {}
            }
            Surface(
                Modifier.size(cellSize).semantics { contentDescription = day.displayDate(); stateDescription = if (marked) markedText else unmarkedText },
                shape = RoundedCornerShape(14.dp),
                color = if (marked) color.copy(alpha = 0.18f) else PaperGreen,
                border = BorderStroke(if (day == today) 2.dp else 1.dp, if (day == today) ActionStrong else SoftGreen)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.number, day.dayOfMonth), style = MaterialTheme.typography.bodyMedium)
                    CheckMark(marked, Modifier.size(12.dp))
                }
            }
        }
    }
}
