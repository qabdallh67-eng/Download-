package com.example.download

import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaDownloadType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MediaDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val dao = AppDatabase.getInstance(appContext).downloadDao()
    private val settingsRepo = SettingsRepository(appContext)

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(KEY_TITLE) ?: "جاري التنزيل..."
        return createForegroundInfo(title, 0, "0%")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L) return@withContext Result.failure()

        val entity = dao.getById(downloadId) ?: return@withContext Result.failure()
        val settings = settingsRepo.settingsFlow.first()

        try {
            setForeground(createForegroundInfo(entity.title, entity.progressPercent, "جاري الاتصال..."))
        } catch (_: Exception) {
            // Best-effort foreground promotion if app is backgrounded without FGS exemption
        }

        val workDir = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AbdullahDownloader")
            .apply { mkdirs() }
        val sanitizedBase = entity.title
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .take(55)
            .ifBlank { "media_$downloadId" }

        val needsAudioExtraction = entity.mediaTypeEnum == MediaDownloadType.AUDIO &&
            !entity.convertToAudioExt.isNullOrBlank()
        val downloadExt = if (needsAudioExtraction) "mp4" else entity.formatExt
        val partFile = File(workDir, "${sanitizedBase}_${downloadId}.$downloadExt.part")
        val targetExt = if (needsAudioExtraction) "m4a" else entity.formatExt
        val finalLocalFile = uniqueTargetFile(workDir, sanitizedBase, targetExt)

        try {
            if (entity.resolvedMediaUrl.startsWith("content://")) {
                copySharedContentUri(
                    uri = Uri.parse(entity.resolvedMediaUrl),
                    partFile = partFile,
                    downloadId = downloadId,
                    title = entity.title,
                    totalExpected = entity.fileSizeBytes
                )
            } else {
                downloadHttpWithResume(
                    url = entity.resolvedMediaUrl,
                    partFile = partFile,
                    downloadId = downloadId,
                    title = entity.title,
                    knownTotal = entity.fileSizeBytes
                )
            }

            if (isStopped) {
                return@withContext Result.success()
            }

            // Convert/extract audio track if requested
            if (needsAudioExtraction) {
                dao.updateProgress(
                    id = downloadId,
                    status = DownloadStatus.DOWNLOADING.name,
                    downloadedBytes = partFile.length(),
                    totalBytes = partFile.length(),
                    speed = 0L,
                    eta = 1L,
                    error = null
                )
                val extracted = AudioConverter.extractAudioToM4a(partFile, finalLocalFile)
                if (extracted) {
                    partFile.delete()
                } else {
                    // Fallback: keep downloaded container intact if source had no separate AAC track
                    partFile.renameTo(finalLocalFile)
                }
            } else {
                if (finalLocalFile.exists()) finalLocalFile.delete()
                if (!partFile.renameTo(finalLocalFile)) {
                    partFile.copyTo(finalLocalFile, overwrite = true)
                    partFile.delete()
                }
            }

            // Also save to user-selected SAF folder if configured
            var savedMediaUri: String? = null
            if (!settings.downloadFolderUri.isNullOrBlank()) {
                savedMediaUri = copyToSafTree(
                    context = applicationContext,
                    treeUriString = settings.downloadFolderUri,
                    localFile = finalLocalFile,
                    mimeType = mimeForExt(targetExt)
                )
            }

            // Publish into Android MediaStore (Gallery / Music / Downloads) on Android 10+
            if (savedMediaUri == null) {
                savedMediaUri = publishToMediaStore(
                    context = applicationContext,
                    localFile = finalLocalFile,
                    isAudio = entity.mediaTypeEnum == MediaDownloadType.AUDIO,
                    mimeType = mimeForExt(targetExt)
                )
            }

            val finalSize = finalLocalFile.length()
            dao.markCompleted(
                id = downloadId,
                finalSize = finalSize,
                localPath = finalLocalFile.absolutePath,
                mediaStoreUri = savedMediaUri,
                completedAt = System.currentTimeMillis()
            )

            DownloadNotificationHelper.showCompletedNotification(
                context = applicationContext,
                downloadId = downloadId,
                title = entity.title,
                subtitle = if (settings.isArabic) "تم التحميل بنجاح • تم الحفظ في ${settings.downloadFolderDisplayName}" else "Download complete • Saved to ${settings.downloadFolderDisplayName}",
                enabled = settings.notificationsEnabled
            )

            Result.success()
        } catch (ce: CancellationException) {
            // Preserve current status if paused or cancelled by user
            val latest = dao.getById(downloadId)
            if (latest?.statusEnum == DownloadStatus.CANCELLED) {
                partFile.delete()
            }
            throw ce
        } catch (e: Exception) {
            val latest = dao.getById(downloadId)
            if (latest?.statusEnum != DownloadStatus.PAUSED && latest?.statusEnum != DownloadStatus.CANCELLED) {
                dao.updateProgress(
                    id = downloadId,
                    status = DownloadStatus.FAILED.name,
                    downloadedBytes = partFile.length(),
                    totalBytes = entity.fileSizeBytes,
                    speed = 0L,
                    eta = 0L,
                    error = e.localizedMessage ?: "تعذر إكمال التحميل"
                )
            }
            Result.failure()
        }
    }

    private suspend fun copySharedContentUri(
        uri: Uri,
        partFile: File,
        downloadId: Long,
        title: String,
        totalExpected: Long
    ) {
        applicationContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(partFile, false).use { output ->
                val buffer = ByteArray(16 * 1024)
                var totalRead = 0L
                var lastUpdate = System.currentTimeMillis()
                var bytesSinceLast = 0L
                while (true) {
                    if (isStopped) return
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    totalRead += read
                    bytesSinceLast += read
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate >= 350L) {
                        val elapsedMs = (now - lastUpdate).coerceAtLeast(1L)
                        val speed = (bytesSinceLast * 1000L) / elapsedMs
                        val total = maxOf(totalExpected, totalRead)
                        val eta = if (speed > 0 && total > totalRead) (total - totalRead) / speed else 0L
                        dao.updateProgress(
                            id = downloadId,
                            status = DownloadStatus.DOWNLOADING.name,
                            downloadedBytes = totalRead,
                            totalBytes = total,
                            speed = speed,
                            eta = eta,
                            error = null
                        )
                        lastUpdate = now
                        bytesSinceLast = 0L
                    }
                }
            }
        } ?: error("تعذر قراءة الملف المشارك من المصدر.")
    }

    private suspend fun downloadHttpWithResume(
        url: String,
        partFile: File,
        downloadId: Long,
        title: String,
        knownTotal: Long
    ) {
        var existingBytes = if (partFile.exists()) partFile.length() else 0L
        val reqBuilder = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=$existingBytes-")
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            if (resp.code == 416) {
                // Already fully downloaded
                return
            }
            if (!resp.isSuccessful) {
                error("HTTP ${resp.code}: ${resp.message}")
            }

            val isPartial = resp.code == 206
            if (!isPartial && existingBytes > 0L) {
                // Server ignored Range header; restart cleanly
                existingBytes = 0L
                partFile.delete()
            }

            val body = resp.body ?: error("استجابة خادم فارغة")
            val contentLen = body.contentLength()
            val totalBytes = when {
                isPartial && contentLen > 0L -> existingBytes + contentLen
                contentLen > 0L -> contentLen
                knownTotal > 0L -> knownTotal
                else -> 0L
            }

            var downloaded = existingBytes
            var lastUpdate = System.currentTimeMillis()
            var bytesSinceLast = 0L
            var smoothedSpeed = 0L

            dao.updateProgress(
                id = downloadId,
                status = DownloadStatus.DOWNLOADING.name,
                downloadedBytes = downloaded,
                totalBytes = totalBytes,
                speed = 0L,
                eta = 0L,
                error = null
            )

            FileOutputStream(partFile, isPartial).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        if (isStopped) return
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLast += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 350L) {
                            val elapsedMs = (now - lastUpdate).coerceAtLeast(1L)
                            val instantSpeed = (bytesSinceLast * 1000L) / elapsedMs
                            smoothedSpeed = if (smoothedSpeed == 0L) instantSpeed else (smoothedSpeed * 6 + instantSpeed * 4) / 10
                            val eta = if (smoothedSpeed > 0L && totalBytes > downloaded) {
                                (totalBytes - downloaded) / smoothedSpeed
                            } else 0L
                            val pct = if (totalBytes > 0L) ((downloaded * 100L) / totalBytes).toInt() else 0

                            dao.updateProgress(
                                id = downloadId,
                                status = DownloadStatus.DOWNLOADING.name,
                                downloadedBytes = downloaded,
                                totalBytes = totalBytes,
                                speed = smoothedSpeed,
                                eta = eta,
                                error = null
                            )
                            runCatching {
                                setForegroundAsync(
                                    createForegroundInfo(
                                        title = title,
                                        progressPercent = pct,
                                        detail = "$pct% • ${formatBytesShort(smoothedSpeed)}/s"
                                    )
                                )
                            }
                            lastUpdate = now
                            bytesSinceLast = 0L
                        }
                    }
                    out.flush()
                }
            }
        }
    }

    private fun createForegroundInfo(title: String, progressPercent: Int, detail: String): ForegroundInfo {
        val notification = DownloadNotificationHelper.buildProgressNotification(
            context = applicationContext,
            workerId = id,
            title = title,
            progressPercent = progressPercent,
            detailText = detail
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    private fun uniqueTargetFile(dir: File, baseName: String, ext: String): File {
        var candidate = File(dir, "$baseName.$ext")
        var idx = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$idx.$ext")
            idx++
        }
        return candidate
    }

    private fun copyToSafTree(
        context: Context,
        treeUriString: String,
        localFile: File,
        mimeType: String
    ): String? = runCatching {
        val treeUri = Uri.parse(treeUriString)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val targetDoc = rootDoc.createFile(mimeType, localFile.name) ?: return null
        context.contentResolver.openOutputStream(targetDoc.uri)?.use { out ->
            localFile.inputStream().use { input -> input.copyTo(out) }
        }
        targetDoc.uri.toString()
    }.getOrNull()

    private fun publishToMediaStore(
        context: Context,
        localFile: File,
        isAudio: Boolean,
        mimeType: String
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val resolver = context.contentResolver
            val collection = if (isAudio) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relPath = if (isAudio) {
                "${Environment.DIRECTORY_MUSIC}/AbdullahDownloader"
            } else {
                "${Environment.DIRECTORY_MOVIES}/AbdullahDownloader"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, localFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val itemUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(itemUri)?.use { out ->
                localFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            itemUri.toString()
        }.getOrNull()
    }

    private fun mimeForExt(ext: String): String = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        else -> "video/mp4"
    }

    private fun formatBytesShort(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_TITLE = "key_title"
    }
}
