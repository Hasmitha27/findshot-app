package com.findshot.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.findshot.data.ScreenshotRepository

/**
 * Indexes every not-yet-indexed image in the background.
 * Safe to re-run: it skips anything already indexed (see getAlreadyIndexedIds),
 * which is also what makes it work correctly when triggered again for
 * newly-added photos later (e.g. from a ContentObserver — see README
 * for how to wire that up next).
 */
class IndexingWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = ScreenshotRepository(applicationContext)
            repo.indexAnyNewImages()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "screenshot_indexing_work"
    }
}
