package com.habitchain.calendar.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NordicMinimal = lightColorScheme(
    primary = ActionStrong,
    onPrimary = PaperWhite,
    primaryContainer = SoftGreen,
    onPrimaryContainer = Ink,
    secondary = Forest,
    onSecondary = PaperWhite,
    secondaryContainer = PaperGreen,
    onSecondaryContainer = Ink,
    background = PaperGreen,
    onBackground = Ink,
    surface = PaperWhite,
    onSurface = Ink,
    surfaceVariant = PaperGreen,
    onSurfaceVariant = MutedInk,
    outline = MutedInk,
    outlineVariant = SoftGreen,
    error = Error,
    onError = PaperWhite,
    errorContainer = ErrorSurface,
    onErrorContainer = Error
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NordicMinimal, typography = AppTypography, content = content)
}
