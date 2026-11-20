package com.example.data.adapters

import android.content.Context
import com.example.data.ai.AiMediaAdvisorRepository
import com.example.data.ai.PlaceholderAiMediaAdvisorRepository
import com.example.data.model.AnalyzedMediaResult

class MediaAnalyzerService(
    private val aiAdvisor: AiMediaAdvisorRepository = PlaceholderAiMediaAdvisorRepository()
) {
    private val adapters: List<MediaSourceAdapter> = listOf(
        DirectUrlAdapter(),
        YouTubeAdapter(),
        VimeoAdapter(),
        FacebookAdapter(),
        TikTokAdapter(),
        InstagramAdapter(),
        XAdapter(),
        TelegramAdapter(),
        WhatsAppAdapter(),
        GenericWebAdapter()
    )

    fun extractFirstUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        if (trimmed.startsWith("content://")) return trimmed
        val match = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE).find(trimmed)
        return match?.value?.trimEnd('.', ',', ';', ')')
    }

    suspend fun analyze(
        context: Context,
        rawInput: String,
        mimeType: String? = null
    ): AnalyzedMediaResult {
        val cleanInput = extractFirstUrl(rawInput) ?: rawInput.trim()
        val adapter = adapters.firstOrNull { it.matches(cleanInput, mimeType) } ?: GenericWebAdapter()
        val result = adapter.analyze(context, cleanInput, mimeType)
        val insight = aiAdvisor.analyzeMediaContext(
            title = result.title,
            platform = result.platform,
            hasVideo = result.videoQualities.isNotEmpty(),
            hasAudio = result.audioFormats.isNotEmpty()
        )
        return result.copy(
            aiInsightAr = insight.summaryAr,
            aiInsightEn = insight.summaryEn,
            isAiPlaceholder = insight.isPlaceholder
        )
    }
}
