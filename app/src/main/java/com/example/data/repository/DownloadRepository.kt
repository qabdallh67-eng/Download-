package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao
) {
    val allDownloads: Flow<List<DownloadEntity>> = dao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = dao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = dao.getCompletedDownloads()
    val completedVideos: Flow<List<DownloadEntity>> = dao.getCompletedVideos()
    val completedAudio: Flow<List<DownloadEntity>> = dao.getCompletedAudio()
    val favorites: Flow<List<DownloadEntity>> = dao.getFavorites()

    suspend fun getById(id: Long): DownloadEntity? = dao.getById(id)

    suspend fun toggleFavorite(entity: DownloadEntity) {
        dao.setFavorite(entity.id, !entity.isFavorite)
    }

    suspend fun renameFile(entity: DownloadEntity, newNameRaw: String): Boolean = withContext(Dispatchers.IO) {
        val cleanName = newNameRaw.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        if (cleanName.isBlank()) return@withContext false

        var updatedPath = entity.localFilePath
        if (!entity.localFilePath.isNullOrBlank()) {
            val oldFile = File(entity.localFilePath)
            if (oldFile.exists() && oldFile.parentFile != null) {
                val target = File(oldFile.parentFile, "$cleanName.${entity.formatExt}")
                if (!target.exists() && oldFile.renameTo(target)) {
                    updatedPath = target.absolutePath
                }
            }
        }
        dao.updateTitleAndPath(entity.id, cleanName, updatedPath)
        true
    }

    suspend fun deleteDownloadAndFile(entity: DownloadEntity) = withContext(Dispatchers.IO) {
        entity.localFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        dao.delete(entity)
    }

    suspend fun clearFinishedHistory() = dao.clearFinishedHistory()

    suspend fun calculateStorageUsageBytes(): Long = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AbdullahDownloader")
        if (!dir.exists()) return@withContext 0L
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearTempPartFiles(): Int = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AbdullahDownloader")
        if (!dir.exists()) return@withContext 0
        var removed = 0
        dir.listFiles()?.filter { it.name.endsWith(".part") }?.forEach {
            if (it.delete()) removed++
        }
        removed
    }

    fun buildShareIntent(entity: DownloadEntity): Intent? {
        val shareUri: Uri? = when {
            !entity.localFilePath.isNullOrBlank() && File(entity.localFilePath).exists() -> {
                runCatching {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(entity.localFilePath)
                    )
                }.getOrNull()
            }
            !entity.mediaStoreUri.isNullOrBlank() -> Uri.parse(entity.mediaStoreUri)
            else -> null
        }

        val mime = if (entity.formatExt == "mp3") "audio/mpeg" else if (entity.formatExt == "m4a") "audio/mp4" else "video/*"
        return if (shareUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, entity.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${entity.title}\n${entity.sourceUrl}")
            }
        }
    }
}
