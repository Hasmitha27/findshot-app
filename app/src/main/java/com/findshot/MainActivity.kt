package com.findshot

import android.Manifest
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.findshot.data.ScreenshotEntity
import com.findshot.data.ScreenshotRepository
import com.findshot.ui.theme.FindshotTheme
import com.findshot.worker.IndexingWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindshotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenshotSearchScreen()
                }
            }
        }
    }
}

@Composable
fun ScreenshotSearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScreenshotRepository(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ScreenshotEntity>>(emptyList()) }
    var isIndexing by remember { mutableStateOf(false) }
    var indexedCount by remember { mutableStateOf(0) }

    val permission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    fun refreshResults(q: String) {
        scope.launch {
            results = repository.search(q)
            indexedCount = repository.indexedCount()
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
                delay(3000)
                refreshResults("")
                isIndexing = false
            }
        }
    }

    // Live sync: index any new photo/screenshot the instant it's added, while open.
    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                scope.launch {
                    repository.indexAnyNewImages()
                    refreshResults(query)
                }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    // Debounced live search: waits 250ms after the last keystroke before querying.
    LaunchedEffect(query, hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        delay(250)
        refreshResults(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "Find the photo\nhiding in your gallery.",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Findshot reads the text inside your screenshots and photos, entirely on this device, so you can search receipts, signs, notes, and memories in seconds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        NeoBrutalSurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Your photos stay on this device",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Every photo is processed on-device — nothing is uploaded to search it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (!hasPermission) {
            NeoBrutalSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = MaterialTheme.colorScheme.primary,
                onClick = { permissionLauncher.launch(permission) }
            ) {
                Text(
                    "Grant photo access",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        } else {
            NeoBrutalSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    isIndexing = true
                    scope.launch {
                        repository.indexAnyNewImages()
                        refreshResults(query)
                        isIndexing = false
                    }
                }
            ) {
                Text(
                    if (isIndexing) "Scanning your gallery…" else "Rescan gallery now",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "New photos are also picked up automatically while Findshot is open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "YOUR GALLERY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Search what you can see", style = MaterialTheme.typography.headlineSmall)
            Text(
                "$indexedCount photos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        if (hasPermission) {
            NeoBrutalSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.surface,
                cornerRadius = 28.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.outline)
                        )
                        if (query.isEmpty()) {
                            Text(
                                "Try \"wifi\", \"invoice\", or \"hill station\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            when {
                isIndexing -> IndexingRow()
                indexedCount == 0 -> EmptyState(
                    title = "Nothing indexed yet",
                    subtitle = "Grant access above and Findshot will start reading your gallery."
                )
                query.isNotBlank() && results.isEmpty() -> EmptyState(
                    title = "No matches",
                    subtitle = "Try different words from what's actually in the photo."
                )
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        items(results) { item ->
                            AsyncImage(
                                model = Uri.parse(item.uriString),
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The signature element: a hard black border + offset drop-shadow, no blur. */
@Composable
private fun NeoBrutalSurface(
    modifier: Modifier = Modifier,
    color: Color,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(borderColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .clip(RoundedCornerShape(cornerRadius))
                .background(color)
                .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun IndexingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Scanning your gallery…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}