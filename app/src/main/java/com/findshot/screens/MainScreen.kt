package com.findshot.screens

import android.Manifest
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.findshot.components.AppHeader
import com.findshot.components.DashboardSection
import com.findshot.components.SearchComponent
import com.findshot.components.SearchResultsSection
import com.findshot.components.timeAgo
import com.findshot.data.ScreenshotEntity
import com.findshot.data.ScreenshotRepository
import com.findshot.ui.OnboardingScreen
import com.findshot.worker.IndexingWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 250L
private const val INDEXING_DELAY_MS = 3_000L
private const val MAX_RECENT_SEARCHES = 4

private val requiredPermission: String
    get() = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

data class RecentSearch(val query: String, val timestampMs: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScreenshotRepository(context) }



    var viewingImage by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var inspecting by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ScreenshotEntity>>(emptyList()) }
    var isIndexing by remember { mutableStateOf(false) }
    var indexedCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var recentSearches by remember { mutableStateOf<List<RecentSearch>>(emptyList()) }

    fun refreshCounts() {
        scope.launch {
            indexedCount = repository.indexedCount()
            totalCount = repository.queryAllImages().size
        }
    }

    fun runSearch(q: String) {
        scope.launch {
            results = repository.search(q)
            if (q.isNotBlank()) {
                recentSearches = (listOf(RecentSearch(q, System.currentTimeMillis())) +
                        recentSearches.filterNot { it.query.equals(q, ignoreCase = true) })
                    .take(MAX_RECENT_SEARCHES)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            isIndexing = true
            val request = OneTimeWorkRequestBuilder<IndexingWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            scope.launch {
                delay(INDEXING_DELAY_MS)
                refreshCounts()
                isIndexing = false
            }
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                scope.launch {
                    repository.indexAnyNewImages()
                    refreshCounts()
                    if (query.isNotBlank()) runSearch(query)
                }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    LaunchedEffect(query, hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        delay(SEARCH_DEBOUNCE_MS)
        runSearch(query)
    }

    if (!hasPermission) {
        OnboardingScreen(onGrantAccess = { permissionLauncher.launch(requiredPermission) })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        AppHeader()
        Spacer(Modifier.height(2.dp))
        Text(
            "Search your gallery, on your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        SearchComponent(
            query = query,
            onQueryChange = { query = it },
            onClear = { query = "" }
        )

        if (query.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            SearchResultsSection(
                query = query,
                results = results,
                viewingImage = viewingImage,
                inspecting = inspecting,
                onViewImage = { viewingImage = it },
                onInspect = { inspecting = it },
                onDismissViewer = { viewingImage = null },
                onDismissInspect = { inspecting = null }
            )
        } else {
            DashboardSection(
                isIndexing = isIndexing,
                indexedCount = indexedCount,
                totalCount = totalCount,
                recentSearches = recentSearches,
                onSuggestionClick = { query = it },
                onRecentClick = { query = it },
                onReindexClick = {
                    isIndexing = true
                    scope.launch {
                        repository.indexAnyNewImages()
                        refreshCounts()
                        isIndexing = false
                    }
                }
            )
        }
    }
}
















