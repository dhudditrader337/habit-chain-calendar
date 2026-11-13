package com.habitchain.calendar.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.habitchain.calendar.R

@Composable
fun SymbolArt(index: Int, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val painter = when (Math.floorMod(index, 5)) {
        0 -> painterResource(R.drawable.sprite_1)
        1 -> painterResource(R.drawable.sprite_2)
        2 -> painterResource(R.drawable.sprite_3)
        3 -> painterResource(R.drawable.sprite_4)
        else -> painterResource(R.drawable.sprite_5)
    }
    Image(painter, null, modifier.size(size), contentScale = ContentScale.Fit)
}
