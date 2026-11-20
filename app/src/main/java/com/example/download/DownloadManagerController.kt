package com.example.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.UserSettings
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaDownloadType
import com.example.data.model.VideoQualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadManagerController(
    private val context: Context,
    private val dao: DownloadDao
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun startDownload(
        analyzed: AnalyzedMediaResult,
        mediaType: MediaDownloadType,
        videoQuality: VideoQualityOption?,
        audioFormat: AudioFormatOption?,
        settings: UserSettings
    ): Long = withContext(Dispatchers.IO) {
        val resolvedUrl: String
        val qualityLabel: String
        val formatExt: String
        val expectedSize: Long
        val convertExt: String?

        if (mediaType == MediaDownloadType.VIDEO) {
            val selected = videoQuality ?: analyzed.videoQualities.firstOrNull()
                ?: error("لا توجد جودة فيديو متاحة لهذا الرابط.")
            resolvedUrl = selected.streamUrl
            qualityLabel = selected.label
            formatExt = extFromMimeOrUrl(selected.mimeType, selected.streamUrl, "mp4")
            expectedSize = selected.estimatedSizeBytes ?: 0L
            convertExt = null
        } else {
            val selected = audioFormat ?: analyzed.audioFormats.firstOrNull()
                ?: error("لا توجد صيغة صوتية متاحة لهذا الرابط.")
            resolvedUrl = selected.streamUrl
            qualityLabel = selected.label
            val isNativeMp3 = selected.mimeType == "audio/mpeg"
            formatExt = if (selected.formatName.equals("MP3", true) && isNativeMp3) "mp3" else "m4a"
            expectedSize = selected.estimatedSizeBytes ?: 0L
            convertExt = if (selected.requiresExtractionFromVideo) formatExt else null
        }

        val entity = DownloadEntity(
            title = analyzed.title,
            sourceUrl = analyzed.sourceUrl,
            resolvedMediaUrl = resolvedUrl,
            platform = analyzed.platform.name,
            mediaType = mediaType.name,
            qualityLabel = qualityLabel,
            formatExt = formatExt,
            thumbnailUrl = analyzed.thumbnailUrl,
            fileSizeBytes = expectedSize,
            status = DownloadStatus.QUEUED.name,
            durationSeconds = analyzed.durationSeconds ?: 0L,
            folderName = settings.downloadFolderDisplayName,
            convertToAudioExt = convertExt
        )

        val id = dao.insert(entity)
        enqueueWorker(id, entity.title, settings.wifiOnly)
        id
    }

    suspend fun pauseDownload(id: Long) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: return@withContext
        dao.updateProgress(
            id = id,
            status = DownloadStatus.PAUSED.name,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.fileSizeBytes,
            speed = 0L,
            eta = 0L,
            error = null
        )
        workManager.cancelUniqueWork(workName(id))
    }

    suspend fun resumeDownload(id: Long, wifiOnly: Boolean) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: return@withContext
        dao.updateProgress(
            id = id,
            status = DownloadStatus.QUEUED.name,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.fileSizeBytes,
            speed = 0L,
            eta = 0L,
            error = null
        )
        enqueueWorker(id, entity.title, wifiOnly)
    }

    suspend fun retryDownload(id: Long, wifiOnly: Boolean) = resumeDownload(id, wifiOnly)

    suspend fun cancelDownload(id: Long) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: return@withContext
        dao.updateProgress(
            id = id,
            status = DownloadStatus.CANCELLED.name,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.fileSizeBytes,
            speed = 0L,
            eta = 0L,
            error = null
        )
        workManager.cancelUniqueWork(workName(id))
    }

    private suspend fun enqueueWorker(downloadId: Long, title: String, wifiOnly: Boolean) {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val input = Data.Builder()
            .putLong(MediaDownloadWorker.KEY_DOWNLOAD_ID, downloadId)
            .putString(MediaDownloadWorker.KEY_TITLE, title)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .addTag("download_$downloadId")
            .build()

        dao.updateWorkerId(downloadId, request.id.toString())
        workManager.enqueueUniqueWork(
            workName(downloadId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(id: Long) = "abdullah_download_$id"

    private fun extFromMimeOrUrl(mime: String, url: String, defaultExt: String): String {
        return when {
            mime.contains("webm") -> "webm"
            mime.contains("quicktime") -> "mov"
            mime.contains("mpeg") -> "mp3"
            mime.contains("mp4") && mime.startsWith("audio") -> "m4a"
            url.substringBefore("?").lowercase().endsWith(".webm") -> "webm"
            url.substringBefore("?").lowercase().endsWith(".mov") -> "mov"
            else -> defaultExt
        }
    }
}
