package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "abdullah_settings")

data class UserSettings(
    val isArabic: Boolean = true,
    val darkMode: Boolean = false,
    val defaultVideoQuality: String = "Full HD – 1080p",
    val defaultAudioFormat: String = "MP3",
    val downloadFolderUri: String? = null,
    val downloadFolderDisplayName: String = "Downloads/AbdullahDownloader",
    val wifiOnly: Boolean = false,
    val notificationsEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val IS_ARABIC = booleanPreferencesKey("is_arabic")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        val DOWNLOAD_FOLDER_NAME = stringPreferencesKey("download_folder_name")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            isArabic = prefs[Keys.IS_ARABIC] ?: true,
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            defaultVideoQuality = prefs[Keys.DEFAULT_VIDEO_QUALITY] ?: "Full HD – 1080p",
            defaultAudioFormat = prefs[Keys.DEFAULT_AUDIO_FORMAT] ?: "MP3",
            downloadFolderUri = prefs[Keys.DOWNLOAD_FOLDER_URI],
            downloadFolderDisplayName = prefs[Keys.DOWNLOAD_FOLDER_NAME] ?: "Downloads/AbdullahDownloader",
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun setArabic(isArabic: Boolean) {
        context.settingsDataStore.edit { it[Keys.IS_ARABIC] = isArabic }
    }

    suspend fun setDarkMode(darkMode: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_MODE] = darkMode }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_VIDEO_QUALITY] = quality }
    }

    suspend fun setDefaultAudioFormat(format: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_AUDIO_FORMAT] = format }
    }

    suspend fun setDownloadFolder(uriString: String?, displayName: String) {
        context.settingsDataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.DOWNLOAD_FOLDER_URI)
            } else {
                prefs[Keys.DOWNLOAD_FOLDER_URI] = uriString
            }
            prefs[Keys.DOWNLOAD_FOLDER_NAME] = displayName
        }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.settingsDataStore.edit { it[Keys.WIFI_ONLY] = wifiOnly }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
