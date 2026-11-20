package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.data.local.UserSettings
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.MediaDownloadType
import com.example.data.model.PlatformType
import com.example.data.model.VideoQualityOption
import com.example.ui.components.CrownPlayLogo
import com.example.ui.components.GlowCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.PlatformIconBadge
import com.example.ui.components.PortraitAvatar
import com.example.ui.components.QuickSharePopupCard
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldRich
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCard
import com.example.ui.theme.SkyHeaderEnd
import com.example.ui.theme.SkyHeaderMid
import com.example.ui.theme.SkyHeaderStart
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.AnalysisUiState

private data class SampleLinkPreset(
    val titleAr: String,
    val titleEn: String,
    val url: String
)

private val publicSampleStreams = listOf(
    SampleLinkPreset(
        titleAr = "فيديو تجريبي HD (Big Buck Bunny)",
        titleEn = "Sample HD Video (Big Buck Bunny)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    ),
    SampleLinkPreset(
        titleAr = "فيديو قصير سريع (For Bigger Blazes)",
        titleEn = "Fast Short Clip (For Bigger Blazes)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    )
)

@Composable
fun HomeScreen(
    strings: AppStrings,
    settings: UserSettings,
    urlInput: String,
    analysisState: AnalysisUiState,
    recentCompleted: List<DownloadEntity>,
    activeDownloads: List<DownloadEntity>,
    onUrlChange: (String) -> Unit,
    onAnalyzeClick: (String) -> Unit,
    onDismissPopup: () -> Unit,
    onStartDownload: (AnalyzedMediaResult, MediaDownloadType, VideoQualityOption?, AudioFormatOption?) -> Unit,
    onPauseResume: (DownloadEntity) -> Unit,
    onPlayMedia: (DownloadEntity) -> Unit,
    onOpenDownloadsTab: () -> Unit,
    onOpenSettingsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == NavyVoid
    val supportedPlatforms = listOf(
        PlatformType.YOUTUBE,
        PlatformType.FACEBOOK,
        PlatformType.TIKTOK,
        PlatformType.INSTAGRAM,
        PlatformType.X,
        PlatformType.VIMEO,
        PlatformType.TELEGRAM,
        PlatformType.WHATSAPP,
        PlatformType.DIRECT_URL
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Header featuring custom Sky-Blue & Gold Artwork + Exact Personal Portrait + Crown Logo
        item {
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_hero_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                if (isDark) {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0A1B42).copy(alpha = 0.78f),
                                            Color(0xFF06112B).copy(alpha = 0.88f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFFFFF).copy(alpha = 0.16f),
                                            Color(0xFFDDF0FF).copy(alpha = 0.56f),
                                            Color(0xFFC2E4FF).copy(alpha = 0.72f)
                                        )
                                    )
                                }
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                PortraitAvatar(size = 86.dp)
                                CrownPlayLogo(
                                    size = 34.dp,
                                    modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.appNameAr,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (isDark) GoldBright else LightTextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = strings.appNameEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isDark) Color.White.copy(alpha = 0.92f) else GoldDeep
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = strings.welcomeMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.88f) else LightTextSecondary
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.78f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.30f) else ElectricBlue.copy(alpha = 0.35f)
                                )
                            ) {
                                IconButton(
                                    onClick = onOpenSettingsTab,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("home_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = strings.navSettings,
                                        tint = if (isDark) Color.White else ElectricBlue
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDark) {
                                ElectricBlue.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.84f)
                            },
                            border = BorderStroke(
                                1.2.dp,
                                if (isDark) GoldBright.copy(alpha = 0.65f) else ElectricBlue.copy(alpha = 0.50f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.welcomeBadge,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDark) Color.White else ElectricBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        // URL Input + Paste + Analyze CTA Card
        item {
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        placeholder = {
                            Text(
                                text = strings.urlHint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = ElectricBlue)
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val pasted = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                    if (pasted.isNotBlank()) {
                                        onUrlChange(pasted)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (strings.isArabic) "الحافظة فارغة" else "Clipboard is empty",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.testTag("home_paste_button")
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = strings.pasteUrl,
                                    tint = if (isDark) GoldBright else GoldRich,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(strings.pasteUrl, color = if (isDark) GoldBright else GoldRich)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else SkyCard.copy(alpha = 0.45f),
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else SkyCard.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_url_input")
                    )

                    Spacer(Modifier.height(12.dp))

                    GradientPrimaryButton(
                        text = strings.analyzeUrl,
                        onClick = { onAnalyzeClick(urlInput) },
                        icon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_analyze_button")
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.sampleLinksTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(publicSampleStreams) { sample ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ElectricBlue.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        onUrlChange(sample.url)
                                        onAnalyzeClick(sample.url)
                                    }
                            ) {
                                Text(
                                    text = if (strings.isArabic) sample.titleAr else sample.titleEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) CyanGlow else ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Share Instructions Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) RoyalPurple.copy(alpha = 0.16f) else Color(0xFFDCEFFF),
                border = BorderStroke(
                    1.dp,
                    if (isDark) VioletBright.copy(alpha = 0.45f) else ElectricBlue.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SkyHeaderStart, SkyHeaderEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.quickShareTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDark) GoldBright else GoldRich,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = strings.quickShareDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Supported Platforms Grid (3 columns) with custom 3D generated icons
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (isDark) GoldBright else GoldRich,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.supportedPlatformsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                supportedPlatforms.chunked(3).forEach { rowPlatforms ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPlatforms.forEach { platform ->
                            GlowCard(
                                glowBorder = false,
                                onClick = {
                                    val hintUrl = when (platform) {
                                        PlatformType.YOUTUBE -> "https://www.youtube.com/watch?v="
                                        PlatformType.FACEBOOK -> "https://www.facebook.com/watch/?v="
                                        PlatformType.TIKTOK -> "https://www.tiktok.com/"
                                        PlatformType.INSTAGRAM -> "https://www.instagram.com/reel/"
                                        PlatformType.X -> "https://x.com/"
                                        PlatformType.VIMEO -> "https://vimeo.com/"
                                        PlatformType.TELEGRAM -> "https://t.me/"
                                        PlatformType.WHATSAPP -> "https://whatsapp.com/"
                                        else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                    }
                                    onUrlChange(hintUrl)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PlatformIconBadge(platform = platform, size = 56.dp)
                                    Spacer(Modifier.height(9.dp))
                                    Text(
                                        text = platform.displayName(strings.isArabic),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Downloads Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentDownloadsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (activeDownloads.isNotEmpty() || recentCompleted.isNotEmpty()) {
                    TextButton(onClick = onOpenDownloadsTab) {
                        Text(strings.navDownloads, color = ElectricBlue)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            if (recentCompleted.isEmpty()) {
                GlowCard(glowBorder = false, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.noRecentDownloads,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentCompleted.take(4).forEach { item ->
                        GlowCard(
                            glowBorder = false,
                            onClick = { onPlayMedia(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlatformIconBadge(platform = item.platformEnum, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.qualityLabel} • ${Formatters.formatBytes(item.fileSizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onPlayMedia(item) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = strings.play,
                                        tint = ElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    // Compact Popup Dialog when Analyzing from Home Screen
    if (analysisState !is AnalysisUiState.Idle) {
        Dialog(
            onDismissRequest = onDismissPopup,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 520.dp),
                contentAlignment = Alignment.Center
            ) {
                val ready = (analysisState as? AnalysisUiState.Ready)?.result
                val errText = (analysisState as? AnalysisUiState.Error)?.let {
                    if (strings.isArabic) it.messageAr else it.messageEn
                }
                QuickSharePopupCard(
                    strings = strings,
                    isLoading = analysisState is AnalysisUiState.Loading,
                    errorMessage = errText,
                    analyzedResult = ready,
                    activeDownload = null,
                    defaultVideoQuality = settings.defaultVideoQuality,
                    defaultAudioFormat = settings.defaultAudioFormat,
                    onClose = onDismissPopup,
                    onStartDownload = { type, vq, af ->
                        if (ready != null) {
                            onStartDownload(ready, type, vq, af)
                            onDismissPopup()
                            onOpenDownloadsTab()
                        }
                    },
                    onPauseResume = onPauseResume,
                    onOpenDownloads = {
                        onDismissPopup()
                        onOpenDownloadsTab()
                    }
                )
            }
        }
    }
}
