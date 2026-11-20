package com.example.data.adapters

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.PlatformType
import com.example.data.model.VideoQualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

internal object AdapterNetwork {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    suspend fun headInfo(url: String): HeadResult = withContext(Dispatchers.IO) {
        val headAttempt = runCatching {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                HeadResult(
                    success = resp.isSuccessful,
                    code = resp.code,
                    contentType = resp.header("Content-Type")?.lowercase().orEmpty(),
                    contentLength = resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L },
                    finalUrl = resp.request.url.toString()
                )
            }
        }.getOrElse {
            HeadResult(success = false, code = 0, contentType = "", contentLength = null, finalUrl = url)
        }

        if (headAttempt.success && headAttempt.contentType.isNotBlank()) {
            return@withContext headAttempt
        }

        // Fallback to a 1-byte Range GET for open CDNs that return 405/403/501 to HEAD requests
        runCatching {
            val rangeReq = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-0")
                .build()
            client.newCall(rangeReq).execute().use { resp ->
                val contentRange = resp.header("Content-Range").orEmpty()
                val totalFromRange = Regex("""/(\d+)$""").find(contentRange)?.groupValues?.getOrNull(1)?.toLongOrNull()
                val len = totalFromRange ?: resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 1L }
                HeadResult(
                    success = resp.isSuccessful || resp.code == 206,
                    code = resp.code,
                    contentType = resp.header("Content-Type")?.lowercase().orEmpty(),
                    contentLength = len,
                    finalUrl = resp.request.url.toString()
                )
            }
        }.getOrElse { headAttempt }
    }

    suspend fun fetchHtmlOrJson(url: String, maxBytes: Int = 512 * 1024): FetchResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val contentType = resp.header("Content-Type")?.lowercase().orEmpty()
                    val contentLength = resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L }
                    val finalUrl = resp.request.url.toString()
                    if (!resp.isSuccessful) {
                        return@withContext FetchResult(
                            success = false,
                            code = resp.code,
                            contentType = contentType,
                            contentLength = contentLength,
                            finalUrl = finalUrl,
                            body = ""
                        )
                    }
                    if (contentType.startsWith("video/") || contentType.startsWith("audio/") ||
                        contentType.contains("application/octet-stream")
                    ) {
                        return@withContext FetchResult(
                            success = true,
                            code = resp.code,
                            contentType = contentType,
                            contentLength = contentLength,
                            finalUrl = finalUrl,
                            body = ""
                        )
                    }
                    val source = resp.body?.source()
                    val bodyStr = if (source != null) {
                        val buffer = okio.Buffer()
                        var total = 0L
                        while (total < maxBytes) {
                            val read = source.read(buffer, 8192L)
                            if (read == -1L) break
                            total += read
                        }
                        buffer.readUtf8()
                    } else ""

                    FetchResult(
                        success = true,
                        code = resp.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        finalUrl = finalUrl,
                        body = bodyStr
                    )
                }
            }.getOrElse {
                FetchResult(
                    success = false,
                    code = 0,
                    contentType = "",
                    contentLength = null,
                    finalUrl = url,
                    body = ""
                )
            }
        }

    fun extractMetaTags(html: String, baseUrl: String = ""): HtmlMetadata {
        fun findMeta(attrValue: String): String? {
            val patterns = listOf(
                Regex("""<meta[^>]+(?:property|name)=["']$attrValue["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']$attrValue["']""", RegexOption.IGNORE_CASE)
            )
            for (p in patterns) {
                val match = p.find(html)
                if (match != null) return decodeHtmlEntities(match.groupValues[1].trim())
            }
            return null
        }

        fun resolveUrl(raw: String?): String? {
            val cleaned = raw?.trim()?.replace("\\u0026", "&")?.replace("\\/", "/") ?: return null
            if (cleaned.isBlank() || cleaned.startsWith("blob:", true) || cleaned.startsWith("data:", true)) return null
            if (cleaned.startsWith("http://", true) || cleaned.startsWith("https://", true)) return cleaned
            if (baseUrl.isBlank()) return null
            return runCatching { java.net.URI(baseUrl).resolve(cleaned).toString() }.getOrNull()
        }

        val title = findMeta("og:title")
            ?: findMeta("twitter:title")
            ?: Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it.trim()) }

        val image = resolveUrl(findMeta("og:image") ?: findMeta("twitter:image"))
        val ogVideo = resolveUrl(
            findMeta("og:video:secure_url")
                ?: findMeta("og:video:url")
                ?: findMeta("og:video")
                ?: findMeta("twitter:player:stream")
        )
        val ogWidth = findMeta("og:video:width")?.toIntOrNull()
        val ogHeight = findMeta("og:video:height")?.toIntOrNull()
        val siteName = findMeta("og:site_name")

        val htmlVideoMatches = Regex(
            """<(?:video|source)[^>]+src=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        ).findAll(html).mapNotNull { resolveUrl(decodeHtmlEntities(it.groupValues[1])) }.toList()

        val jsonLdVideo = Regex(
            """"contentUrl"\s*:\s*"([^"]+)"""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1)?.let { resolveUrl(it) }

        val rawMp4InHtml = Regex(
            """https?://[^\s"'<>\\]+\.(?:mp4|m4v|webm)(?:\?[^\s"'<>\\]*)?""",
            RegexOption.IGNORE_CASE
        ).findAll(html).mapNotNull { resolveUrl(it.value) }.distinct().toList()

        val candidateVideos = buildList {
            if (!ogVideo.isNullOrBlank()) add(ogVideo)
            if (!jsonLdVideo.isNullOrBlank()) add(jsonLdVideo)
            addAll(htmlVideoMatches)
            addAll(rawMp4InHtml.take(6))
        }.distinct()

        return HtmlMetadata(
            title = title,
            imageUrl = image,
            siteName = siteName,
            videoWidth = ogWidth,
            videoHeight = ogHeight,
            candidateVideoUrls = candidateVideos
        )
    }

    fun isDirectMediaExtensionOrStream(url: String): Boolean {
        val clean = url.substringBefore("?").lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".m4v") || clean.endsWith(".mov") ||
            clean.endsWith(".webm") || clean.endsWith(".mkv") || clean.endsWith(".mp3") ||
            clean.endsWith(".m4a") || clean.endsWith(".aac") || clean.endsWith(".wav")
    }

    fun qualityLabelForHeight(height: Int?): Pair<String, Int> = when {
        height == null -> "HD – 720p" to 720
        height >= 4000 -> "8K – 4320p" to 4320
        height >= 2000 -> "4K – 2160p" to 2160
        height >= 1300 -> "2K – 1440p" to 1440
        height >= 1000 -> "Full HD – 1080p" to 1080
        height >= 680 -> "HD – 720p" to 720
        else -> "SD – 480p" to 480
    }

    fun guessTitleFromUrl(url: String): String {
        val path = runCatching { Uri.parse(url).lastPathSegment }.getOrNull().orEmpty()
        val decoded = runCatching { URLDecoder.decode(path, "UTF-8") }.getOrDefault(path)
        return decoded.substringBeforeLast(".").replace('_', ' ').replace('-', ' ').trim()
            .ifBlank { "فيديو وسائط" }
    }

    private fun decodeHtmlEntities(input: String): String = input
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

internal data class HeadResult(
    val success: Boolean,
    val code: Int,
    val contentType: String,
    val contentLength: Long?,
    val finalUrl: String
)

internal data class FetchResult(
    val success: Boolean,
    val code: Int,
    val contentType: String,
    val contentLength: Long?,
    val finalUrl: String,
    val body: String
)

internal data class HtmlMetadata(
    val title: String?,
    val imageUrl: String?,
    val siteName: String?,
    val videoWidth: Int?,
    val videoHeight: Int?,
    val candidateVideoUrls: List<String>
)

internal fun buildAudioOptionsFromStream(
    streamUrl: String,
    isAlreadyAudio: Boolean,
    mimeType: String,
    sizeBytes: Long?
): List<AudioFormatOption> {
    val isMp3Source = mimeType.contains("mpeg") || streamUrl.substringBefore("?").lowercase().endsWith(".mp3")
    val m4aSize = if (isAlreadyAudio) sizeBytes else sizeBytes?.let { (it * 0.22).toLong().coerceAtLeast(120_000L) }
    val mp3Size = if (isAlreadyAudio) sizeBytes else sizeBytes?.let { (it * 0.25).toLong().coerceAtLeast(140_000L) }

    return listOf(
        AudioFormatOption(
            id = "mp3_320",
            formatName = "MP3",
            bitrateKbps = 320,
            streamUrl = streamUrl,
            mimeType = if (isMp3Source) "audio/mpeg" else "audio/mp4",
            estimatedSizeBytes = mp3Size,
            requiresExtractionFromVideo = !isAlreadyAudio && !isMp3Source
        ),
        AudioFormatOption(
            id = "m4a_192",
            formatName = "M4A",
            bitrateKbps = 192,
            streamUrl = streamUrl,
            mimeType = "audio/mp4",
            estimatedSizeBytes = m4aSize,
            requiresExtractionFromVideo = !isAlreadyAudio
        )
    )
}

class DirectUrlAdapter : MediaSourceAdapter {
    override val platform = PlatformType.DIRECT_URL

    override fun matches(urlOrUri: String, mimeType: String?): Boolean {
        if (!urlOrUri.startsWith("http://", true) && !urlOrUri.startsWith("https://", true)) return false
        return AdapterNetwork.isDirectMediaExtensionOrStream(urlOrUri)
    }

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        val head = AdapterNetwork.headInfo(urlOrUri)
        val effectiveType = head.contentType.ifBlank {
            when {
                urlOrUri.lowercase().contains(".mp3") -> "audio/mpeg"
                urlOrUri.lowercase().contains(".m4a") -> "audio/mp4"
                else -> "video/mp4"
            }
        }
        val title = AdapterNetwork.guessTitleFromUrl(head.finalUrl)
        val isAudio = effectiveType.startsWith("audio/")
        val isVideo = effectiveType.startsWith("video/") || (!isAudio && AdapterNetwork.isDirectMediaExtensionOrStream(head.finalUrl))

        if (!head.success && head.code in listOf(401, 403, 404, 410)) {
            return AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                isDownloadable = false,
                restrictionReasonAr = "تعذر الوصول إلى الرابط المباشر (رمز الاستجابة ${head.code}). قد يتطلب الرابط تصريح دخول أو انتهت صلاحيته.",
                restrictionReasonEn = "Cannot access direct link (HTTP ${head.code}). The link may require authorization or has expired."
            )
        }

        if (!isVideo && !isAudio) {
            return AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                isDownloadable = false,
                restrictionReasonAr = "الرابط لا يشير إلى ملف فيديو أو صوت مباشر مدعوم (${head.contentType.ifBlank { "غير معروف" }}).",
                restrictionReasonEn = "URL does not point to a supported direct video or audio stream (${head.contentType.ifBlank { "unknown" }})."
            )
        }

        val qualities = if (isVideo) {
            val (label, h) = guessQualityFromUrlOrSize(head.finalUrl, head.contentLength)
            listOf(
                VideoQualityOption(
                    id = "direct_${h}",
                    label = label,
                    heightPx = h,
                    streamUrl = head.finalUrl,
                    mimeType = effectiveType.ifBlank { "video/mp4" },
                    estimatedSizeBytes = head.contentLength,
                    isHighestAvailable = true
                )
            )
        } else emptyList()

        val audios = buildAudioOptionsFromStream(
            streamUrl = head.finalUrl,
            isAlreadyAudio = isAudio,
            mimeType = effectiveType,
            sizeBytes = head.contentLength
        )

        return AnalyzedMediaResult(
            sourceUrl = urlOrUri,
            platform = platform,
            title = title,
            isDownloadable = true,
            videoQualities = qualities,
            audioFormats = audios
        )
    }

    private fun guessQualityFromUrlOrSize(url: String, sizeBytes: Long?): Pair<String, Int> {
        val lower = url.lowercase()
        return when {
            "4320" in lower || "8k" in lower -> "8K – 4320p" to 4320
            "2160" in lower || "4k" in lower -> "4K – 2160p" to 2160
            "1440" in lower || "2k" in lower -> "2K – 1440p" to 1440
            "1080" in lower || "fhd" in lower -> "Full HD – 1080p" to 1080
            "720" in lower || "hd" in lower -> "HD – 720p" to 720
            "480" in lower || "sd" in lower -> "SD – 480p" to 480
            sizeBytes != null && sizeBytes > 60_000_000L -> "Full HD – 1080p" to 1080
            else -> "HD – 720p" to 720
        }
    }
}

class YouTubeAdapter : MediaSourceAdapter {
    override val platform = PlatformType.YOUTUBE

    override fun matches(urlOrUri: String, mimeType: String?): Boolean {
        val lower = urlOrUri.lowercase()
        return "youtube.com" in lower || "youtu.be" in lower
    }

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        val oembedUrl = "https://www.youtube.com/oembed?url=${Uri.encode(urlOrUri)}&format=json"
        val oembed = AdapterNetwork.fetchHtmlOrJson(oembedUrl)
        var title = "YouTube Video"
        var author: String? = null
        var thumb: String? = null
        if (oembed.success && oembed.body.isNotBlank()) {
            runCatching {
                val json = JSONObject(oembed.body)
                title = json.optString("title", title)
                author = json.optString("author_name").takeIf { it.isNotBlank() }
                thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
            }
        }
        return AnalyzedMediaResult(
            sourceUrl = urlOrUri,
            platform = platform,
            title = title,
            author = author,
            thumbnailUrl = thumb,
            isDownloadable = false,
            restrictionReasonAr = "تم استرداد معلومات الفيديو بنجاح من YouTube، لكن سياسة المنصة وحماية حقوق النشر تمنع استخراج روابط البث المحمية بدون ترخيص رسمي (YouTube Premium / API معتمد).",
            restrictionReasonEn = "Video info retrieved from YouTube, but platform terms and stream protection prohibit unauthorized direct media extraction."
        )
    }
}

class VimeoAdapter : MediaSourceAdapter {
    override val platform = PlatformType.VIMEO

    override fun matches(urlOrUri: String, mimeType: String?): Boolean =
        "vimeo.com" in urlOrUri.lowercase()

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        val videoId = Regex("""vimeo\.com/(?:video/)?(\d+)""").find(urlOrUri)?.groupValues?.getOrNull(1)
        val oembed = AdapterNetwork.fetchHtmlOrJson("https://vimeo.com/api/oembed.json?url=${Uri.encode(urlOrUri)}")
        var title = "Vimeo Video"
        var author: String? = null
        var thumb: String? = null
        var duration: Long? = null

        if (oembed.success && oembed.body.isNotBlank()) {
            runCatching {
                val json = JSONObject(oembed.body)
                title = json.optString("title", title)
                author = json.optString("author_name").takeIf { it.isNotBlank() }
                thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
                duration = json.optLong("duration", 0L).takeIf { it > 0L }
            }
        }

        // Check public player config if exposed by creator without private/DRM lock
        val qualities = mutableListOf<VideoQualityOption>()
        if (videoId != null) {
            val configResp = AdapterNetwork.fetchHtmlOrJson("https://player.vimeo.com/video/$videoId/config")
            if (configResp.success && configResp.body.contains("\"progressive\"")) {
                runCatching {
                    val cfg = JSONObject(configResp.body)
                    val progressive = cfg.optJSONObject("request")?.optJSONObject("files")?.optJSONArray("progressive")
                    if (progressive != null) {
                        for (i in 0 until progressive.length()) {
                            val item = progressive.getJSONObject(i)
                            val streamUrl = item.optString("url")
                            val height = item.optInt("height", 720)
                            val size = item.optLong("size", 0L).takeIf { it > 0L }
                            if (streamUrl.startsWith("http")) {
                                val (label, normalizedH) = AdapterNetwork.qualityLabelForHeight(height)
                                qualities += VideoQualityOption(
                                    id = "vimeo_$normalizedH",
                                    label = label,
                                    heightPx = normalizedH,
                                    streamUrl = streamUrl,
                                    estimatedSizeBytes = size
                                )
                            }
                        }
                    }
                }
            }
        }

        val sorted = qualities.distinctBy { it.heightPx }.sortedByDescending { it.heightPx }
            .mapIndexed { idx, opt -> opt.copy(isHighestAvailable = idx == 0) }

        return if (sorted.isNotEmpty()) {
            AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                author = author,
                thumbnailUrl = thumb,
                durationSeconds = duration,
                isDownloadable = true,
                videoQualities = sorted,
                audioFormats = buildAudioOptionsFromStream(
                    streamUrl = sorted.first().streamUrl,
                    isAlreadyAudio = false,
                    mimeType = "video/mp4",
                    sizeBytes = sorted.first().estimatedSizeBytes
                )
            )
        } else {
            AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                author = author,
                thumbnailUrl = thumb,
                durationSeconds = duration,
                isDownloadable = false,
                restrictionReasonAr = "تم جلب بيانات الفيديو من Vimeo، ولكن صاحب الفيديو أو المنصة لم يفعّل رابط تنزيل تقدمي عام (Progressive Public Stream) لهذا المقطع.",
                restrictionReasonEn = "Retrieved Vimeo metadata, but this video does not expose an authorized public progressive download stream."
            )
        }
    }
}

open class OpenGraphPlatformAdapter(
    override val platform: PlatformType,
    private val domains: List<String>
) : MediaSourceAdapter {
    override fun matches(urlOrUri: String, mimeType: String?): Boolean {
        val lower = urlOrUri.lowercase()
        return domains.any { it in lower }
    }

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        val fetch = AdapterNetwork.fetchHtmlOrJson(urlOrUri)
        if (!fetch.success) {
            return AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = platform.displayNameAr,
                isDownloadable = false,
                restrictionReasonAr = "لم نتمكن من قراءة محتوى الصفحة من ${platform.displayNameAr} (HTTP ${fetch.code}). قد يكون المنشور خاصاً أو يتطلب تسجيل دخول.",
                restrictionReasonEn = "Could not inspect ${platform.displayNameEn} page (HTTP ${fetch.code}). The post may be private or require sign-in."
            )
        }

        if (fetch.contentType.startsWith("video/") || fetch.contentType.startsWith("audio/")) {
            return DirectUrlAdapter().analyze(context, fetch.finalUrl, fetch.contentType)
        }

        val meta = AdapterNetwork.extractMetaTags(fetch.body, fetch.finalUrl)
        val title = meta.title?.takeIf { it.isNotBlank() } ?: "${platform.displayNameEn} Media"

        // Verify candidate direct media URLs via HEAD / Range probe and collect all available open qualities
        val verifiedOptions = mutableListOf<VideoQualityOption>()
        for ((index, candidate) in meta.candidateVideoUrls.take(6).withIndex()) {
            val head = AdapterNetwork.headInfo(candidate)
            val looksLikeDirectVideo = head.contentType.startsWith("video/") ||
                (head.contentType.contains("application/octet-stream") && AdapterNetwork.isDirectMediaExtensionOrStream(candidate))
            if (head.success && looksLikeDirectVideo) {
                val guessedHeight = Regex("""(?:4320|2160|1440|1080|720|480)""").find(candidate)?.value?.toIntOrNull()
                    ?: meta.videoHeight
                val (label, h) = AdapterNetwork.qualityLabelForHeight(guessedHeight)
                verifiedOptions += VideoQualityOption(
                    id = "${platform.name.lowercase()}_${h}_$index",
                    label = label,
                    heightPx = h,
                    streamUrl = head.finalUrl,
                    mimeType = head.contentType.ifBlank { "video/mp4" },
                    estimatedSizeBytes = head.contentLength
                )
            }
        }

        val sorted = verifiedOptions.distinctBy { it.heightPx }.sortedByDescending { it.heightPx }
            .mapIndexed { idx, opt -> opt.copy(isHighestAvailable = idx == 0) }

        return if (sorted.isNotEmpty()) {
            AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                thumbnailUrl = meta.imageUrl,
                isDownloadable = true,
                videoQualities = sorted,
                audioFormats = buildAudioOptionsFromStream(
                    streamUrl = sorted.first().streamUrl,
                    isAlreadyAudio = false,
                    mimeType = "video/mp4",
                    sizeBytes = sorted.first().estimatedSizeBytes
                )
            )
        } else {
            AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = title,
                thumbnailUrl = meta.imageUrl,
                isDownloadable = false,
                restrictionReasonAr = "منصة ${platform.displayNameAr} تحمي هذا الفيديو عبر تشفير البث أو جلسة تسجيل الدخول ولا توفر رابط فيديو مفتوح متاحاً للتنزيل المصرح به.",
                restrictionReasonEn = "${platform.displayNameEn} protects this media behind authenticated or encrypted streams with no authorized open download URL."
            )
        }
    }
}

class FacebookAdapter : OpenGraphPlatformAdapter(
    PlatformType.FACEBOOK,
    listOf("facebook.com", "fb.watch", "fb.com")
)

class TikTokAdapter : OpenGraphPlatformAdapter(
    PlatformType.TIKTOK,
    listOf("tiktok.com", "vm.tiktok.com")
)

class InstagramAdapter : OpenGraphPlatformAdapter(
    PlatformType.INSTAGRAM,
    listOf("instagram.com", "instagr.am")
)

class XAdapter : OpenGraphPlatformAdapter(
    PlatformType.X,
    listOf("x.com", "twitter.com", "t.co")
)

class TelegramAdapter : MediaSourceAdapter {
    override val platform = PlatformType.TELEGRAM
    private val webDelegate = OpenGraphPlatformAdapter(PlatformType.TELEGRAM, listOf("t.me", "telegram.me"))

    override fun matches(urlOrUri: String, mimeType: String?): Boolean {
        val lower = urlOrUri.lowercase()
        return "t.me" in lower || "telegram.me" in lower ||
            (lower.startsWith("content://") && "telegram" in lower)
    }

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        if (urlOrUri.startsWith("content://")) {
            return SharedContentUriAdapter(PlatformType.TELEGRAM).analyze(context, urlOrUri, mimeType)
        }
        return webDelegate.analyze(context, urlOrUri, mimeType)
    }
}

class WhatsAppAdapter : MediaSourceAdapter {
    override val platform = PlatformType.WHATSAPP

    override fun matches(urlOrUri: String, mimeType: String?): Boolean {
        val lower = urlOrUri.lowercase()
        return "whatsapp" in lower || (lower.startsWith("content://") && (mimeType?.startsWith("video/") == true || mimeType?.startsWith("audio/") == true))
    }

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        if (urlOrUri.startsWith("content://")) {
            return SharedContentUriAdapter(PlatformType.WHATSAPP).analyze(context, urlOrUri, mimeType)
        }
        return AnalyzedMediaResult(
            sourceUrl = urlOrUri,
            platform = platform,
            title = "WhatsApp Shared Link",
            isDownloadable = false,
            restrictionReasonAr = "مشاركة الوسائط من واتساب تعمل عند مشاركة ملف الفيديو أو الصوت مباشرة من المحادثة إلى التطبيق.",
            restrictionReasonEn = "WhatsApp media saving works when sharing a video or audio file directly from a chat into this app."
        )
    }
}

class SharedContentUriAdapter(
    override val platform: PlatformType
) : MediaSourceAdapter {
    override fun matches(urlOrUri: String, mimeType: String?): Boolean =
        urlOrUri.startsWith("content://")

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(urlOrUri)
            val resolvedMime = mimeType ?: context.contentResolver.getType(uri) ?: "video/mp4"
            var displayName = "وسائط مشتركة"
            var sizeBytes: Long? = null

            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx)?.substringBeforeLast('.') ?: displayName
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sizeBytes = cursor.getLong(sizeIdx).takeIf { it > 0L }
                        }
                    }
                }
            }

            val isAudio = resolvedMime.startsWith("audio/")
            val qualities = if (!isAudio) {
                listOf(
                    VideoQualityOption(
                        id = "shared_original",
                        label = "Full HD – 1080p",
                        heightPx = 1080,
                        streamUrl = urlOrUri,
                        mimeType = resolvedMime,
                        estimatedSizeBytes = sizeBytes,
                        isHighestAvailable = true
                    )
                )
            } else emptyList()

            val audios = buildAudioOptionsFromStream(
                streamUrl = urlOrUri,
                isAlreadyAudio = isAudio,
                mimeType = resolvedMime,
                sizeBytes = sizeBytes
            )

            AnalyzedMediaResult(
                sourceUrl = urlOrUri,
                platform = platform,
                title = displayName,
                isDownloadable = true,
                videoQualities = qualities,
                audioFormats = audios,
                sharedContentUri = urlOrUri
            )
        }
}

class GenericWebAdapter : MediaSourceAdapter {
    override val platform = PlatformType.OTHER
    private val delegate = OpenGraphPlatformAdapter(PlatformType.OTHER, emptyList())

    override fun matches(urlOrUri: String, mimeType: String?): Boolean = true

    override suspend fun analyze(context: Context, urlOrUri: String, mimeType: String?): AnalyzedMediaResult {
        if (urlOrUri.startsWith("content://")) {
            return SharedContentUriAdapter(PlatformType.OTHER).analyze(context, urlOrUri, mimeType)
        }
        return delegate.analyze(context, urlOrUri, mimeType)
    }
}
