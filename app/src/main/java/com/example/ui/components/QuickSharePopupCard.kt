package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.AudioFormatOption
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaDownloadType
import com.example.data.model.VideoQualityOption
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRich
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SkyCard
import com.example.ui.theme.SkyHeaderEnd
import com.example.ui.theme.SkyHeaderStart
import com.example.ui.theme.SkySurface
import com.example.ui.theme.VioletBright

@Composable
fun QuickSharePopupCard(
    strings: AppStrings,
    isLoading: Boolean,
    errorMessage: String?,
    analyzedResult: AnalyzedMediaResult?,
    activeDownload: DownloadEntity?,
    defaultVideoQuality: String,
    defaultAudioFormat: String,
    onClose: () -> Unit,
    onStartDownload: (MediaDownloadType, VideoQualityOption?, AudioFormatOption?) -> Unit,
    onPauseResume: (DownloadEntity) -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == NavyVoid
    val containerColor = if (isDark) NavyDeep else Color(0xFFF0F8FF)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = ElectricBlue, spotColor = CyanGlow),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.6.dp,
            brush = Brush.linearGradient(listOf(CyanGlow, ElectricBlue, VioletBright))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Top Branding & Close Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrownPlayLogo(size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appNameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) GoldBright else GoldRich,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = strings.appNameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("popup_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.closeLabel,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricBlue)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = strings.analyzing,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AmberWarning.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = AmberWarning)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.closeLabel)
                    }
                }

                activeDownload != null -> {
                    LivePopupDownloadProgress(
                        strings = strings,
                        item = activeDownload,
                        isDark = isDark,
                        onPauseResume = { onPauseResume(activeDownload) },
                        onOpenDownloads = onOpenDownloads,
                        onClose = onClose
                    )
                }

                analyzedResult != null -> {
                    AnalyzedMediaSelectionBody(
                        strings = strings,
                        result = analyzedResult,
                        isDark = isDark,
                        defaultVideoQuality = defaultVideoQuality,
                        defaultAudioFormat = defaultAudioFormat,
                        onStartDownload = onStartDownload,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzedMediaSelectionBody(
    strings: AppStrings,
    result: AnalyzedMediaResult,
    isDark: Boolean,
    defaultVideoQuality: String,
    defaultAudioFormat: String,
    onStartDownload: (MediaDownloadType, VideoQualityOption?, AudioFormatOption?) -> Unit,
    onClose: () -> Unit
) {
    val initialType = if (result.videoQualities.isNotEmpty()) MediaDownloadType.VIDEO else MediaDownloadType.AUDIO
    var selectedType by remember(result.sourceUrl) { mutableStateOf(initialType) }
    var selectedVideo by remember(result.sourceUrl) {
        mutableStateOf(
            result.videoQualities.firstOrNull { it.label == defaultVideoQuality }
                ?: result.videoQualities.firstOrNull()
        )
    }
    var selectedAudio by remember(result.sourceUrl) {
        mutableStateOf(
            result.audioFormats.firstOrNull { it.formatName.equals(defaultAudioFormat, true) }
                ?: result.audioFormats.firstOrNull()
        )
    }

    val cardBg = if (isDark) NavyCard else SkySurface
    val cardBorder = if (isDark) ElectricBlue.copy(alpha = 0.28f) else SkyBorder

    // Media Thumbnail + Title + Source Platform Card
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) NavySurface else SkyCard),
                contentAlignment = Alignment.Center
            ) {
                if (!result.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    PlatformIconBadge(platform = result.platform, size = 50.dp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformIconBadge(platform = result.platform, size = 24.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${strings.fromPlatformPrefix} ${result.platform.displayName(strings.isArabic)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    if (!result.isDownloadable) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AmberWarning.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = AmberWarning)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (strings.isArabic) "توضيح حول مصدر الفيديو" else "Source Access Limitation",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isDark) AmberWarning else GoldRich,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = result.restrictionReason(strings.isArabic) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        AiGuidanceCard(strings = strings, result = result, isDark = isDark)

        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(strings.closeLabel)
        }
        return
    }

    // "ماذا تريد تنزيله؟"
    Text(
        text = strings.whatToDownload,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val videoEnabled = result.videoQualities.isNotEmpty()
        val videoSelected = selectedType == MediaDownloadType.VIDEO
        MediaTypeButton(
            label = strings.videoOption,
            icon = Icons.Rounded.Videocam,
            selected = videoSelected,
            enabled = videoEnabled,
            isDark = isDark,
            onClick = { selectedType = MediaDownloadType.VIDEO },
            modifier = Modifier
                .weight(1f)
                .testTag("popup_type_video")
        )

        val audioSelected = selectedType == MediaDownloadType.AUDIO
        MediaTypeButton(
            label = strings.audioOnlyOption,
            icon = Icons.Rounded.MusicNote,
            selected = audioSelected,
            enabled = result.audioFormats.isNotEmpty(),
            isDark = isDark,
            onClick = { selectedType = MediaDownloadType.AUDIO },
            modifier = Modifier
                .weight(1f)
                .testTag("popup_type_audio")
        )
    }

    Spacer(Modifier.height(14.dp))

    // Quality / Audio Selector
    AnimatedVisibility(visible = selectedType == MediaDownloadType.VIDEO) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isDark) GoldBright else GoldRich,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.selectQualityTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDark) GoldBright else GoldRich,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            result.videoQualities.forEach { quality ->
                val isSelected = selectedVideo?.id == quality.id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isSelected && isDark -> ElectricBlue.copy(alpha = 0.22f)
                        isSelected -> Color(0xFFD6ECFF)
                        else -> cardBg
                    },
                    border = BorderStroke(
                        1.2.dp,
                        if (isSelected) ElectricBlue else cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedVideo = quality }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                if (quality.isHighestAvailable) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = GoldBright.copy(alpha = 0.22f),
                                        border = BorderStroke(0.8.dp, GoldRich.copy(alpha = 0.65f))
                                    ) {
                                        Text(
                                            text = strings.highestAvailableBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) GoldBright else GoldRich,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (quality.estimatedSizeBytes != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = Formatters.formatBytes(quality.estimatedSizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = selectedType == MediaDownloadType.AUDIO) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.selectAudioFormatTitle,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDark) GoldBright else GoldRich,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                result.audioFormats.forEach { format ->
                    val isSelected = selectedAudio?.id == format.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected && isDark -> ElectricBlue.copy(alpha = 0.25f)
                            isSelected -> Color(0xFFD6ECFF)
                            else -> cardBg
                        },
                        border = BorderStroke(
                            1.2.dp,
                            if (isSelected) ElectricBlue else cardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedAudio = format }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = format.formatName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${format.bitrateKbps} kbps",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                            if (format.estimatedSizeBytes != null) {
                                Text(
                                    text = Formatters.formatBytes(format.estimatedSizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    AiGuidanceCard(strings = strings, result = result, isDark = isDark)

    Spacer(Modifier.height(16.dp))

    GradientPrimaryButton(
        text = strings.startDownload,
        onClick = { onStartDownload(selectedType, selectedVideo, selectedAudio) },
        icon = {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("popup_start_download")
    )
}

@Composable
private fun AiGuidanceCard(strings: AppStrings, result: AnalyzedMediaResult, isDark: Boolean) {
    val insight = if (strings.isArabic) result.aiInsightAr else result.aiInsightEn
    if (insight.isNullOrBlank()) return
    Spacer(Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) RoyalPurple.copy(alpha = 0.16f) else Color(0xFFE6EEFF),
        border = BorderStroke(1.dp, VioletBright.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) GoldBright else GoldRich,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (strings.isArabic) "مستشار الوسائط الذكي" else "Smart Media Advisor",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) GoldBright else GoldRich,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = insight,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (result.isAiPlaceholder) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.aiNoticeBanner,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MediaTypeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unselectedBg = if (isDark) NavyCard else SkyCard
    val bgBrush = when {
        !enabled -> Brush.horizontalGradient(listOf(unselectedBg.copy(alpha = 0.45f), unselectedBg.copy(alpha = 0.45f)))
        selected -> Brush.horizontalGradient(listOf(SkyHeaderStart, ElectricBlue, SkyHeaderEnd))
        else -> Brush.horizontalGradient(listOf(unselectedBg, unselectedBg))
    }
    val contentTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(
                width = 1.2.dp,
                color = if (selected) CyanGlow else SkyBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentTint
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentTint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LivePopupDownloadProgress(
    strings: AppStrings,
    item: DownloadEntity,
    isDark: Boolean,
    onPauseResume: () -> Unit,
    onOpenDownloads: () -> Unit,
    onClose: () -> Unit
) {
    val isDone = item.statusEnum == DownloadStatus.COMPLETED
    val isPaused = item.statusEnum == DownloadStatus.PAUSED
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isDone) strings.downloadSuccess else item.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))

        if (!isDone) {
            LinearProgressIndicator(
                progress = { item.progressFraction },
                color = ElectricBlue,
                trackColor = if (isDark) NavyCard else SkyCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${Formatters.formatBytes(item.downloadedBytes)} / ${Formatters.formatBytes(item.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.progressPercent}% • ${Formatters.formatSpeed(item.speedBytesPerSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onPauseResume,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPaused) strings.resume else strings.pause)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldSuccess.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.downloadSuccess,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.savedIn} ${item.folderName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(strings.closeLabel)
            }
            GradientPrimaryButton(
                text = strings.viewInDownloads,
                onClick = onOpenDownloads,
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}
