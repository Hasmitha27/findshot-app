package com.findshot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val NeoBrutalColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = Amber,
    background = Cream,
    onBackground = InkBlack,
    surface = SearchWhite,
    onSurface = InkBlack,
    surfaceVariant = Amber,
    onSurfaceVariant = MutedText,
    outline = InkBlack
)

@Composable
fun FindshotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeoBrutalColors,
        typography = FindshotTypography,
        content = content
    )
}