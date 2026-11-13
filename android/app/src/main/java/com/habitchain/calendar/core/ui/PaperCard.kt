package com.habitchain.calendar.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.habitchain.calendar.R
import com.habitchain.calendar.core.theme.*

@Composable
fun PaperCard(modifier: Modifier = Modifier, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = PaperWhite.copy(alpha = 0.95f), border = BorderStroke(1.dp, SoftGreen.copy(alpha = 0.8f)), shadowElevation = 3.dp) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun StatCard(value: Int, labelRes: Int, modifier: Modifier = Modifier) {
    PaperCard(modifier) {
        Text(stringResource(R.string.number, value), style = MaterialTheme.typography.headlineMedium, color = Forest)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
    }
}

@Composable
fun InlineStatus(messageRes: Int, error: Boolean = false) {
    Text(stringResource(messageRes), color = if (error) Error else MutedInk, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
fun BottomActionPanel(hintRes: Int, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = PaperWhite.copy(alpha = 0.97f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SoftGreen), shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(hintRes), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            content()
        }
    }
}
