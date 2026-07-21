package com.findshot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScreenshotEntity)

    @Query("SELECT mediaId FROM indexed_screenshots")
    suspend fun getAllIndexedIds(): List<Long>

    @Query(
        """
        SELECT * FROM indexed_screenshots
        WHERE ocrText LIKE '%' || :query || '%'
        ORDER BY dateAdded DESC
        """
    )
    suspend fun searchByText(query: String): List<ScreenshotEntity>

    @Query("SELECT * FROM indexed_screenshots ORDER BY dateAdded DESC")
    suspend fun getAll(): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM indexed_screenshots")
    suspend fun count(): Int
}
