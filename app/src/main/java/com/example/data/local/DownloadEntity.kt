package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaDownloadType
import com.example.data.model.PlatformType

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val resolvedMediaUrl: String,
    val platform: String = PlatformType.DIRECT_URL.name,
    val mediaType: String = MediaDownloadType.VIDEO.name,
    val qualityLabel: String,
    val formatExt: String,
    val thumbnailUrl: String? = null,
    val localFilePath: String? = null,
    val mediaStoreUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = DownloadStatus.QUEUED.name,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false,
    val durationSeconds: Long = 0L,
    val folderName: String = "Downloads/AbdullahDownloader",
    val workerId: String? = null,
    val convertToAudioExt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val platformEnum: PlatformType
        get() = runCatching { PlatformType.valueOf(platform) }.getOrDefault(PlatformType.OTHER)

    val mediaTypeEnum: MediaDownloadType
        get() = runCatching { MediaDownloadType.valueOf(mediaType) }.getOrDefault(MediaDownloadType.VIDEO)

    val statusEnum: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED)

    val progressFraction: Float
        get() = if (fileSizeBytes > 0L) {
            (downloadedBytes.toFloat() / fileSizeBytes.toFloat()).coerceIn(0f, 1f)
        } else if (statusEnum == DownloadStatus.COMPLETED) {
            1f
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)
}
