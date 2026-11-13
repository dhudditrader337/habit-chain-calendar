package com.habitchain.calendar.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.R
import com.habitchain.calendar.core.theme.*
import java.time.LocalDate

@Composable
fun CalendarDayCell(date: LocalDate, marked: Boolean, selected: Boolean, today: Boolean, enabled: Boolean, cellSize: Dp, onSelect: (LocalDate) -> Unit) {
    val status = stringResource(if (!enabled) R.string.unavailable_day else if (marked) R.string.marked else R.string.not_marked)
    val selectedText = stringResource(R.string.selected)
    TextButton({ onSelect(date) }, Modifier.size(cellSize).semantics { contentDescription = date.displayDate(); stateDescription = if (selected) "$status, $selectedText" else status },
        enabled = enabled, contentPadding = PaddingValues(4.dp), shape = RoundedCornerShape(14.dp)) {
        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), color = if (!enabled) NordicMist else if (marked) SoftGreen else PaperWhite,
            border = BorderStroke(if (selected || today) 2.dp else 1.dp, if (selected) Ink else if (today) ActionStrong else SoftGreen)) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.number, date.dayOfMonth), style = MaterialTheme.typography.bodyLarge, color = if (enabled) Ink else MutedInk.copy(alpha = 0.65f))
                CheckMark(marked, Modifier.size(10.dp).align(Alignment.TopEnd).padding(1.dp))
            }
        }
    }
}
