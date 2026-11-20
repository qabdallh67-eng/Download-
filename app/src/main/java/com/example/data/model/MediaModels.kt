package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PlatformType(
    val displayNameAr: String,
    val displayNameEn: String,
    val brandColor: Color,
    val shortBadge: String
) {
    YOUTUBE("YouTube", "YouTube", Color(0xFFFF1F1F), "YT"),
    FACEBOOK("Facebook", "Facebook", Color(0xFF1877F2), "FB"),
    TIKTOK("TikTok", "TikTok", Color(0xFF00F2EA), "TT"),
    INSTAGRAM("Instagram", "Instagram", Color(0xFFE1306C), "IG"),
    X("X", "X", Color(0xFF94A3B8), "X"),
    VIMEO("Vimeo", "Vimeo", Color(0xFF1AB7EA), "VM"),
    TELEGRAM("Telegram", "Telegram", Color(0xFF229ED9), "TG"),
    WHATSAPP("WhatsApp", "WhatsApp", Color(0xFF25D366), "WA"),
    DIRECT_URL("رابط مباشر", "Direct URL", Color(0xFF1E88FF), "URL"),
    OTHER("مصدر ويب", "Web Source", Color(0xFF9D4EDD), "WEB");

    fun displayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class MediaDownloadType {
    VIDEO,
    AUDIO
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class VideoQualityOption(
    val id: String,
    val label: String,         // e.g. "Full HD – 1080p"
    val heightPx: Int,         // 4320, 2160, 1440, 1080, 720, 480
    val streamUrl: String,
    val mimeType: String = "video/mp4",
    val estimatedSizeBytes: Long? = null,
    val isHighestAvailable: Boolean = false
)

data class AudioFormatOption(
    val id: String,
    val formatName: String,    // "MP3" or "M4A"
    val bitrateKbps: Int,      // e.g. 320, 192, 128
    val streamUrl: String,
    val mimeType: String,
    val estimatedSizeBytes: Long? = null,
    val requiresExtractionFromVideo: Boolean = false
) {
    val label: String get() = "$formatName • ${bitrateKbps} kbps"
}

data class AnalyzedMediaResult(
    val sourceUrl: String,
    val platform: PlatformType,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long? = null,
    val isDownloadable: Boolean,
    val restrictionReasonAr: String? = null,
    val restrictionReasonEn: String? = null,
    val videoQualities: List<VideoQualityOption> = emptyList(),
    val audioFormats: List<AudioFormatOption> = emptyList(),
    val sharedContentUri: String? = null,
    val aiInsightAr: String? = null,
    val aiInsightEn: String? = null,
    val isAiPlaceholder: Boolean = true
) {
    fun restrictionReason(isArabic: Boolean): String? =
        if (isArabic) restrictionReasonAr else restrictionReasonEn
}
