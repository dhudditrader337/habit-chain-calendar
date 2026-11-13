package com.habitchain.calendar.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.R
import com.habitchain.calendar.core.theme.*
import com.habitchain.calendar.domain.model.HabitSummary

@Composable
fun HabitCard(summary: HabitSummary, symbol: Int, sample: Boolean, saving: Boolean, onToggle: () -> Unit, onCalendar: () -> Unit, onDetails: () -> Unit) {
    val status = stringResource(if (summary.markedToday) R.string.marked else R.string.not_marked)
    val toggleLabel = stringResource(R.string.toggle_today, summary.habit.name)
    val color by animateColorAsState(if (summary.markedToday) SoftGreen else PaperGreen, label = "marked day")
    PaperCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(5.dp).height(54.dp).background(summary.habit.color.tint(), RoundedCornerShape(4.dp)))
            SymbolArt(symbol, 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sample) Text(stringResource(R.string.example_label), style = MaterialTheme.typography.labelSmall, color = Forest)
                Text(stringResource(R.string.user_text, summary.habit.name), style = MaterialTheme.typography.titleMedium)
                Text(if (summary.currentChain > 0) stringResource(R.string.chain_days, summary.currentChain) else stringResource(R.string.fresh_start), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            if (!sample) FilledTonalButton(onToggle, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = toggleLabel; stateDescription = status }, enabled = !saving,
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = color)) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    CheckMark(summary.markedToday, Modifier.fillMaxSize())
                    if (!summary.markedToday) Text(stringResource(R.string.unmarked_symbol))
                }
            }
        }
        if (!sample) {
            Text(if (summary.markedToday) stringResource(R.string.today_marked) else if (summary.currentChain > 0) stringResource(R.string.keep_chain, summary.currentChain) else stringResource(R.string.fresh_start), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onCalendar, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.calendar)) }
                TextButton(onDetails, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.details)) }
            }
        }
    }
}
