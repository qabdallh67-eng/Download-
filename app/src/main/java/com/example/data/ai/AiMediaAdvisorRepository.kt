package com.example.data.ai

import com.example.data.model.PlatformType

data class AiMediaInsight(
    val summaryAr: String,
    val summaryEn: String,
    val recommendedPresetAr: String,
    val recommendedPresetEn: String,
    val isPlaceholder: Boolean = true
)

interface AiMediaAdvisorRepository {
    suspend fun analyzeMediaContext(
        title: String,
        platform: PlatformType,
        hasVideo: Boolean,
        hasAudio: Boolean
    ): AiMediaInsight
}

/**
 * Placeholder implementation used when Firebase AI Logic is not provisioned/enabled.
 * Clearly marks all returned insights as offline sample guidance and surfaces an
 * in-app notice that live Gemini intelligence requires enabling Firebase.
 */
class PlaceholderAiMediaAdvisorRepository : AiMediaAdvisorRepository {
    override suspend fun analyzeMediaContext(
        title: String,
        platform: PlatformType,
        hasVideo: Boolean,
        hasAudio: Boolean
    ): AiMediaInsight {
        val summaryAr = when (platform) {
            PlatformType.DIRECT_URL ->
                "نموذج تجريبي: تم فحص الرابط المباشر «$title». يُنصح باختيار أعلى دقة MP4 متاحة أو استخراج M4A للبودكاست والمقاطع الصوتية."
            PlatformType.VIMEO, PlatformType.FACEBOOK, PlatformType.INSTAGRAM,
            PlatformType.TIKTOK, PlatformType.X, PlatformType.YOUTUBE ->
                "نموذج تجريبي: منصة ${platform.displayNameAr} قد تفرض قيوداً على الروابط المحمية أو الخاصة؛ تقتصر الجودات الظاهرة على التدفقات المصرّح بها فقط."
            PlatformType.TELEGRAM, PlatformType.WHATSAPP ->
                "نموذج تجريبي: الوسائط المشاركة محلياً جاهزة للحفظ الفوري أو التحويل إلى صوت M4A/MP3."
            PlatformType.OTHER ->
                "نموذج تجريبي: تم تحليل الصفحة والبحث عن وسوم الوسائط المفتوحة المتاحة."
        }

        val summaryEn = when (platform) {
            PlatformType.DIRECT_URL ->
                "Sample Mode: Analyzed direct link \"$title\". Recommended: highest available MP4 quality or M4A audio extraction."
            PlatformType.VIMEO, PlatformType.FACEBOOK, PlatformType.INSTAGRAM,
            PlatformType.TIKTOK, PlatformType.X, PlatformType.YOUTUBE ->
                "Sample Mode: ${platform.displayNameEn} may restrict protected streams; only authorized accessible qualities are shown."
            PlatformType.TELEGRAM, PlatformType.WHATSAPP ->
                "Sample Mode: Shared local media is ready for immediate saving or M4A/MP3 audio conversion."
            PlatformType.OTHER ->
                "Sample Mode: Inspected page metadata for publicly accessible media streams."
        }

        val presetAr = if (hasVideo) "Full HD – 1080p (أو أعلى جودة متاحة)" else "M4A • 192 kbps"
        val presetEn = if (hasVideo) "Full HD – 1080p (or highest available)" else "M4A • 192 kbps"

        return AiMediaInsight(
            summaryAr = summaryAr,
            summaryEn = summaryEn,
            recommendedPresetAr = presetAr,
            recommendedPresetEn = presetEn,
            isPlaceholder = true
        )
    }
}
