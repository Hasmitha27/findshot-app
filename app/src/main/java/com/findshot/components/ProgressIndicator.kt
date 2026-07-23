package com.findshot.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val PROGRESS_INDICATOR_SIZE = 60.dp

@Composable
fun IndexingProgressIndicator(progress: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(PROGRESS_INDICATOR_SIZE)
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp
        )
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}