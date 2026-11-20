package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserSettings
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.MediaDownloadType
import com.example.data.model.VideoQualityOption
import com.example.ui.components.QuickSharePopupCard
import com.example.ui.strings.AppStrings
import com.example.ui.theme.AbdullahTheme
import kotlinx.coroutines.launch

/**
 * Compact floating Share Receiver Activity.
 * When the user presses Share inside Facebook, YouTube, TikTok, Instagram, Telegram, WhatsApp,
 * or any browser and selects "عبدالله أبو قاعود", Android opens this translucent floating popup
 * directly instead of launching the full main homepage.
 */
class QuickShareActivity : ComponentActivity() {

    private val incomingPayload = mutableStateOf<Pair<String, String?>>(Pair("", null))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingPayload.value = extractPayload(intent)

        val app = application as AbdullahApplication

        setContent {
            val settings by app.settingsRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = UserSettings())
            val strings = remember(settings.isArabic) { AppStrings(settings.isArabic) }
            val scope = rememberCoroutineScope()

            val payload = incomingPayload.value
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var analyzedResult by remember { mutableStateOf<AnalyzedMediaResult?>(null) }
            var activeDownloadId by remember { mutableStateOf<Long?>(null) }

            val activeDownloadFlow = remember(activeDownloadId) {
                activeDownloadId?.let { app.database.downloadDao().observeById(it) }
            }
            val activeDownload = activeDownloadFlow?.collectAsStateWithLifecycle(initialValue = null)?.value

            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* Best effort */ }

            LaunchedEffect(payload) {
                val (raw, mime) = payload
                if (raw.isBlank()) {
                    isLoading = false
                    errorMessage = if (settings.isArabic) {
                        "لم يتم العثور على رابط أو ملف وسائط في المحتوى المشارك."
                    } else {
                        "No video link or media file was found in the shared content."
                    }
                } else {
                    isLoading = true
                    errorMessage = null
                    analyzedResult = null
                    activeDownloadId = null
                    runCatching {
                        app.mediaAnalyzerService.analyze(this@QuickShareActivity, raw, mime)
                    }.onSuccess { result ->
                        analyzedResult = result
                        isLoading = false
                    }.onFailure { err ->
                        isLoading = false
                        errorMessage = err.localizedMessage ?: "Failed to inspect shared link."
                    }
                }
            }

            fun triggerDownload(
                mediaType: MediaDownloadType,
                videoQuality: VideoQualityOption?,
                audioFormat: AudioFormatOption?
            ) {
                val result = analyzedResult ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@QuickShareActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                scope.launch {
                    val id = app.downloadController.startDownload(
                        analyzed = result,
                        mediaType = mediaType,
                        videoQuality = videoQuality,
                        audioFormat = audioFormat,
                        settings = settings
                    )
                    activeDownloadId = id
                }
            }

            AbdullahTheme(darkTheme = settings.darkMode, isArabic = settings.isArabic) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .safeDrawingPadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finish() }
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        QuickSharePopupCard(
                            strings = strings,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            analyzedResult = analyzedResult,
                            activeDownload = activeDownload,
                            defaultVideoQuality = settings.defaultVideoQuality,
                            defaultAudioFormat = settings.defaultAudioFormat,
                            onClose = { finish() },
                            onStartDownload = { type, vq, af -> triggerDownload(type, vq, af) },
                            onPauseResume = { item ->
                                scope.launch {
                                    if (item.status == "PAUSED") {
                                        app.downloadController.resumeDownload(item.id, settings.wifiOnly)
                                    } else {
                                        app.downloadController.pauseDownload(item.id)
                                    }
                                }
                            },
                            onOpenDownloads = {
                                startActivity(
                                    Intent(this@QuickShareActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        putExtra(MainActivity.EXTRA_OPEN_TAB, "downloads")
                                    }
                                )
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingPayload.value = extractPayload(intent)
    }

    private fun extractPayload(intent: Intent?): Pair<String, String?> {
        if (intent == null) return "" to null
        val mime = intent.type
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (streamUri != null) {
                    streamUri.toString() to mime
                } else {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    sharedText to mime
                }
            }
            Intent.ACTION_VIEW -> {
                intent.dataString.orEmpty() to mime
            }
            else -> "" to null
        }
    }
}
