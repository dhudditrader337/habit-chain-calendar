package com.habitchain.calendar.core.theme

import androidx.compose.ui.graphics.Color
import com.habitchain.calendar.domain.model.HabitColor

val PaperGreen = Color(0xFFF2F6ED)
val PaperWhite = Color(0xFFFFFFFF)
val NordicWhite = Color(0xFFFAFAFA)
val NordicMist = Color(0xFFF2F2F4)
val Primary = Color(0xFF6AAB2A)
val Accent = Color(0xFF2886E0)
val Ink = Color(0xFF1E2418)
val SoftGreen = Color(0xFFB1DDB0)
val MutedInk = Color(0xFF4B5841)
val ActionStrong = Color(0xFF1D5F9B)
val ActionDeep = Color(0xFF164A7A)
val Forest = Color(0xFF396715)
val Error = Color(0xFFB3261E)
val ErrorSurface = Color(0xFFFCE9E7)

fun HabitColor.tint(): Color = when (this) {
    HabitColor.Leaf -> Primary
    HabitColor.Sky -> Accent
    HabitColor.Forest -> Forest
    HabitColor.Ink -> Ink
}
