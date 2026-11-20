package com.example

import android.app.Application
import com.example.data.adapters.MediaAnalyzerService
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.DownloadRepository
import com.example.download.DownloadManagerController
import com.example.download.DownloadNotificationHelper

class AbdullahApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(this, database.downloadDao())
    }
    val downloadController: DownloadManagerController by lazy {
        DownloadManagerController(this, database.downloadDao())
    }
    val mediaAnalyzerService: MediaAnalyzerService by lazy { MediaAnalyzerService() }

    override fun onCreate() {
        super.onCreate()
        DownloadNotificationHelper.ensureChannels(this)
    }
}
