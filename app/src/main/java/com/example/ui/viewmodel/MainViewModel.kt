package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AbdullahApplication
import com.example.data.local.DownloadEntity
import com.example.data.local.UserSettings
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.MediaDownloadType
import com.example.data.model.VideoQualityOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Loading(val input: String) : AnalysisUiState
    data class Ready(val result: AnalyzedMediaResult) : AnalysisUiState
    data class Error(val messageAr: String, val messageEn: String) : AnalysisUiState
}

enum class FileCategory { VIDEOS, AUDIO, FAVORITES, RECENT }
enum class FileSortOrder { NEWEST, NAME, SIZE }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AbdullahApplication
    private val settingsRepo = app.settingsRepository
    private val downloadRepo = app.downloadRepository
    private val downloadController = app.downloadController
    private val analyzer = app.mediaAnalyzerService

    val settings: StateFlow<UserSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    val allDownloads: StateFlow<List<DownloadEntity>> = downloadRepo.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeDownloads: StateFlow<List<DownloadEntity>> = downloadRepo.activeDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedDownloads: StateFlow<List<DownloadEntity>> = downloadRepo.completedDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedVideos: StateFlow<List<DownloadEntity>> = downloadRepo.completedVideos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedAudio: StateFlow<List<DownloadEntity>> = downloadRepo.completedAudio.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites: StateFlow<List<DownloadEntity>> = downloadRepo.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _fileCategory = MutableStateFlow(FileCategory.VIDEOS)
    val fileCategory: StateFlow<FileCategory> = _fileCategory.asStateFlow()

    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    private val _fileSortOrder = MutableStateFlow(FileSortOrder.NEWEST)
    val fileSortOrder: StateFlow<FileSortOrder> = _fileSortOrder.asStateFlow()

    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    private val _activePlayerPlaylist = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val activePlayerPlaylist: StateFlow<List<DownloadEntity>> = _activePlayerPlaylist.asStateFlow()

    private val _activePlayerIndex = MutableStateFlow(0)
    val activePlayerIndex: StateFlow<Int> = _activePlayerIndex.asStateFlow()

    init {
        refreshStorageUsage()
    }

    fun updateUrlInput(value: String) {
        _urlInput.value = value
    }

    fun analyzeUrl(rawInput: String = _urlInput.value, mimeType: String? = null) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            _analysisState.value = AnalysisUiState.Error(
                messageAr = "يرجى إدخال أو لصق رابط الفيديو أولاً.",
                messageEn = "Please enter or paste a video URL first."
            )
            return
        }
        _urlInput.value = trimmed
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Loading(trimmed)
            runCatching {
                analyzer.analyze(app, trimmed, mimeType)
            }.onSuccess { analyzed ->
                _analysisState.value = AnalysisUiState.Ready(analyzed)
            }.onFailure { err ->
                _analysisState.value = AnalysisUiState.Error(
                    messageAr = "تعذر تحليل الرابط: ${err.localizedMessage ?: "خطأ في الاتصال"}",
                    messageEn = "Failed to analyze link: ${err.localizedMessage ?: "Connection error"}"
                )
            }
        }
    }

    fun dismissAnalysisPopup() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun enqueueDownload(
        analyzed: AnalyzedMediaResult,
        mediaType: MediaDownloadType,
        videoQuality: VideoQualityOption?,
        audioFormat: AudioFormatOption?,
        onEnqueued: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = downloadController.startDownload(
                analyzed = analyzed,
                mediaType = mediaType,
                videoQuality = videoQuality,
                audioFormat = audioFormat,
                settings = settings.value
            )
            refreshStorageUsage()
            onEnqueued(id)
        }
    }

    fun pauseDownload(id: Long) = viewModelScope.launch {
        downloadController.pauseDownload(id)
    }

    fun resumeDownload(id: Long) = viewModelScope.launch {
        downloadController.resumeDownload(id, settings.value.wifiOnly)
    }

    fun retryDownload(id: Long) = viewModelScope.launch {
        downloadController.retryDownload(id, settings.value.wifiOnly)
    }

    fun cancelDownload(id: Long) = viewModelScope.launch {
        downloadController.cancelDownload(id)
    }

    fun toggleFavorite(entity: DownloadEntity) = viewModelScope.launch {
        downloadRepo.toggleFavorite(entity)
    }

    fun renameFile(entity: DownloadEntity, newName: String) = viewModelScope.launch {
        downloadRepo.renameFile(entity, newName)
    }

    fun deleteDownload(entity: DownloadEntity) = viewModelScope.launch {
        downloadRepo.deleteDownloadAndFile(entity)
        refreshStorageUsage()
    }

    fun clearFinishedHistory() = viewModelScope.launch {
        downloadRepo.clearFinishedHistory()
    }

    fun setFileCategory(category: FileCategory) {
        _fileCategory.value = category
    }

    fun setFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    fun setFileSortOrder(order: FileSortOrder) {
        _fileSortOrder.value = order
    }

    fun openPlayer(item: DownloadEntity, playlist: List<DownloadEntity>) {
        val list = playlist.ifEmpty { listOf(item) }
        val idx = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        _activePlayerPlaylist.value = list
        _activePlayerIndex.value = idx
    }

    fun refreshStorageUsage() = viewModelScope.launch {
        _storageUsedBytes.value = downloadRepo.calculateStorageUsageBytes()
    }

    fun clearTempFiles(onCleared: (Int) -> Unit) = viewModelScope.launch {
        val count = downloadRepo.clearTempPartFiles()
        refreshStorageUsage()
        onCleared(count)
    }

    fun setArabic(isArabic: Boolean) = viewModelScope.launch {
        settingsRepo.setArabic(isArabic)
    }

    fun setDarkMode(darkMode: Boolean) = viewModelScope.launch {
        settingsRepo.setDarkMode(darkMode)
    }

    fun setDefaultVideoQuality(quality: String) = viewModelScope.launch {
        settingsRepo.setDefaultVideoQuality(quality)
    }

    fun setDefaultAudioFormat(format: String) = viewModelScope.launch {
        settingsRepo.setDefaultAudioFormat(format)
    }

    fun setDownloadFolder(uri: Uri?, displayName: String) = viewModelScope.launch {
        settingsRepo.setDownloadFolder(uri?.toString(), displayName)
    }

    fun setWifiOnly(wifiOnly: Boolean) = viewModelScope.launch {
        settingsRepo.setWifiOnly(wifiOnly)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setNotificationsEnabled(enabled)
    }

    fun buildShareFileIntent(entity: DownloadEntity) = downloadRepo.buildShareIntent(entity)
}
