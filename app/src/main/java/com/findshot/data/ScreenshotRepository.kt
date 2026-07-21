package com.findshot.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class MediaImage(val id: Long, val uri: Uri, val dateAdded: Long)

class ScreenshotRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).screenshotDao()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Queries every image MediaStore knows about (photos + screenshots).
     * To restrict to screenshots only, filter by RELATIVE_PATH containing
     * "Screenshots" — left broad here so "hill station" style photo
     * queries work too, per our discussion.
     */
    fun queryAllImages(): List<MediaImage> {
        val images = mutableListOf<MediaImage>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                images.add(MediaImage(id, uri, dateAdded))
            }
        }
        return images
    }

    suspend fun getAlreadyIndexedIds(): Set<Long> = dao.getAllIndexedIds().toSet()

    /** Runs on-device OCR on one image and stores the extracted text. */
    suspend fun indexImage(media: MediaImage) {
        val text = try {
            val inputImage = InputImage.fromFilePath(context, media.uri)
            val result = recognizer.process(inputImage).await()
            result.text
        } catch (e: Exception) {
            // Unreadable/corrupt image — still record it (empty text)
            // so indexing doesn't retry it forever.
            ""
        }

        dao.upsert(
            ScreenshotEntity(
                mediaId = media.id,
                uriString = media.uri.toString(),
                ocrText = text,
                dateAdded = media.dateAdded
            )
        )
    }

    /**
     * Indexes any images not yet indexed. Used both by the background
     * WorkManager job (on launch) and by the live ContentObserver
     * (while the app is open and a new photo/screenshot appears).
     * Returns how many new images were indexed.
     */
    suspend fun indexAnyNewImages(): Int {
        val alreadyIndexed = getAlreadyIndexedIds()
        val allImages = queryAllImages()
        val toIndex = allImages.filter { it.id !in alreadyIndexed }
        toIndex.forEach { media -> indexImage(media) }
        return toIndex.size
    }

    /**
     * Searches by extracting meaningful keywords from the (possibly full-sentence)
     * query, then ranking results by how many of those keywords appear in each
     * image's OCR text — so "wifi password" scores a match on both words higher
     * than one matching only "wifi".
     */
    /**
     * Searches by extracting keywords from the query, then matching each one
     * against OCR text — case-insensitively, and also trying a naive singular
     * form (stripping a trailing "s") so "passwords" matches OCR text that
     * only contains "password". This is simple substring/stemming, not real
     * NLP lemmatization — deliberately kept lightweight and dependency-free.
     */
    suspend fun search(query: String): List<ScreenshotEntity> {
        if (query.isBlank()) return dao.getAll()

        val keywords = QueryParser.extractKeywords(query)
        if (keywords.isEmpty()) return emptyList()

        val all = dao.getAll()
        return all
            .map { entity ->
                val textLower = entity.ocrText.lowercase()
                val textCompact = textLower.replace(Regex("\\s+"), "")

                val matchCount = keywords.count { kw ->
                    val kwLower = kw.lowercase().trim()
                    val singular = if (kwLower.endsWith("s") && kwLower.length > 3)
                        kwLower.dropLast(1) else kwLower

                    textLower.contains(kwLower) || textLower.contains(singular) ||
                            textCompact.contains(kwLower) || textCompact.contains(singular)
                }
                entity to matchCount
            }
            .filter { (_, matchCount) -> matchCount > 0 }
            .sortedByDescending { (_, matchCount) -> matchCount }
            .map { (entity, _) -> entity }
    }
    suspend fun getAllIndexed(): List<ScreenshotEntity> = dao.getAll()

    suspend fun indexedCount(): Int = dao.count()
}
