package com.findshot.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findshot.screens.RecentSearch

@Composable
fun DashboardSection(
    isIndexing: Boolean,
    indexedCount: Int,
    totalCount: Int,
    recentSearches: List<RecentSearch>,
    onSuggestionClick: (String) -> Unit,
    onRecentClick: (String) -> Unit,
    onReindexClick: () -> Unit
) {
    Spacer(Modifier.height(24.dp))
    Text("Suggested Searches", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    SuggestedSearchesGrid(onSuggestionClick = onSuggestionClick)
    Spacer(Modifier.height(24.dp))
    IndexingCard(
        isIndexing = isIndexing,
        indexedCount = indexedCount,
        totalCount = totalCount,
        onClick = onReindexClick
    )
    if (recentSearches.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        RecentSearchesList(recentSearches = recentSearches, onItemClick = onRecentClick)
    }
}