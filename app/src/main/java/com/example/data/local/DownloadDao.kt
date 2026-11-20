package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND mediaType = 'VIDEO' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedVideos(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND mediaType = 'AUDIO' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedAudio(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isFavorite = 1 AND status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getFavorites(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Delete
    suspend fun delete(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE downloads SET title = :newTitle, localFilePath = :newPath WHERE id = :id")
    suspend fun updateTitleAndPath(id: Long, newTitle: String, newPath: String?)

    @Query(
        "UPDATE downloads SET status = :status, downloadedBytes = :downloadedBytes, " +
            "fileSizeBytes = :totalBytes, speedBytesPerSec = :speed, etaSeconds = :eta, errorMessage = :error WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Long,
        status: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speed: Long,
        eta: Long,
        error: String?
    )

    @Query(
        "UPDATE downloads SET status = 'COMPLETED', downloadedBytes = :finalSize, " +
            "fileSizeBytes = :finalSize, localFilePath = :localPath, mediaStoreUri = :mediaStoreUri, " +
            "speedBytesPerSec = 0, etaSeconds = 0, errorMessage = null, completedAt = :completedAt WHERE id = :id"
    )
    suspend fun markCompleted(
        id: Long,
        finalSize: Long,
        localPath: String?,
        mediaStoreUri: String?,
        completedAt: Long
    )

    @Query("UPDATE downloads SET workerId = :workerId WHERE id = :id")
    suspend fun updateWorkerId(id: Long, workerId: String?)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearFinishedHistory()
}
