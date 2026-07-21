package com.findshot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per indexed image.
 * `ocrText` powers Milestone 1 search (exact/substring text match).
 * A `embedding: FloatArray` column gets added here in Milestone 2
 * once the semantic model is wired in, without changing anything else.
 */
@Entity(tableName = "indexed_screenshots")
data class ScreenshotEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val ocrText: String,
    val dateAdded: Long
)
