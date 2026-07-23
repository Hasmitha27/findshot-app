package com.findshot.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.findshot.data.ScreenshotEntity
import com.findshot.screens.RecentSearch


private const val GRID_COLUMNS = 3
private val RESULTS_GRID_HEIGHT = 420.dp


private val SUGGESTED_SEARCHES = listOf(
    SuggestionItem(Icons.Filled.Lock, "Passwords"),
    SuggestionItem(Icons.Filled.ReceiptLong, "Receipts"),
    SuggestionItem(Icons.Filled.Description, "Invoices"),
    SuggestionItem(Icons.Filled.Folder, "Documents"),
    SuggestionItem(Icons.Filled.Flight, "Travel"),
    SuggestionItem(Icons.Filled.Image, "Screenshots")
)

@Composable
fun SearchComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text(
                    "Search anything in your gallery…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultsSection(
    query: String,
    results: List<ScreenshotEntity>,
    viewingImage: ScreenshotEntity?,
    inspecting: ScreenshotEntity?,
    onViewImage: (ScreenshotEntity) -> Unit,
    onInspect: (ScreenshotEntity) -> Unit,
    onDismissViewer: () -> Unit,
    onDismissInspect: () -> Unit
) {
    if (results.isEmpty()) {
        Text(
            "No matches for \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(RESULTS_GRID_HEIGHT)
    ) {
        items(results) { item ->
            AsyncImage(
                model = Uri.parse(item.uriString),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onViewImage(item) },
                        onLongClick = { onInspect(item) }
                    )
            )
        }
    }

    viewingImage?.let { item ->
        FullImageViewer(imageUri = item.uriString, onDismiss = onDismissViewer)
    }

    inspecting?.let { item ->
        ExtractedTextDialog(text = item.ocrText, onDismiss = onDismissInspect)
    }
}

@Composable
fun SuggestedSearchesGrid(onSuggestionClick: (String) -> Unit) {

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SUGGESTED_SEARCHES.chunked(GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { suggestion ->
                    SuggestionComponent(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion.label.lowercase()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSearchRow(recent: RecentSearch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(recent.query, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            timeAgo(recent.timestampMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecentSearchesList(
    recentSearches: List<RecentSearch>,
    onItemClick: (String) -> Unit
) {
    Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        recentSearches.forEach { recent ->
            RecentSearchRow(recent = recent, onClick = { onItemClick(recent.query) })
        }
    }
}

