package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.ui.components.GlowCard
import com.example.ui.components.PlatformIconBadge
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess

private enum class DownloadFilter { ALL, ACTIVE, COMPLETED }

@Composable
fun DownloadsScreen(
    strings: AppStrings,
    allDownloads: List<DownloadEntity>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onPlay: (DownloadEntity) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }
    val filtered = remember(allDownloads, filter) {
        when (filter) {
            DownloadFilter.ALL -> allDownloads
            DownloadFilter.ACTIVE -> allDownloads.filter {
                it.statusEnum in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)
            }
            DownloadFilter.COMPLETED -> allDownloads.filter { it.statusEnum == DownloadStatus.COMPLETED }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.navDownloads,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (allDownloads.any { it.statusEnum in listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED) }) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.clearHistory)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == DownloadFilter.ALL,
                onClick = { filter = DownloadFilter.ALL },
                label = { Text("${strings.allDownloads} (${allDownloads.size})") }
            )
            FilterChip(
                selected = filter == DownloadFilter.ACTIVE,
                onClick = { filter = DownloadFilter.ACTIVE },
                label = { Text(strings.activeDownloads) }
            )
            FilterChip(
                selected = filter == DownloadFilter.COMPLETED,
                onClick = { filter = DownloadFilter.COMPLETED },
                label = { Text(strings.completedDownloads) }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.emptyDownloads,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    DownloadProgressCard(
                        strings = strings,
                        item = item,
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onCancel = { onCancel(item.id) },
                        onRetry = { onRetry(item.id) },
                        onPlay = { onPlay(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    strings: AppStrings,
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit
) {
    val status = item.statusEnum
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        PlatformIconBadge(platform = item.platformEnum, size = 40.dp)
                    }
                    if (status == DownloadStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${item.qualityLabel} • ${item.formatExt.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel, tint = CrimsonError)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (status) {
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        color = if (status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
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
                        val etaText = if (item.etaSeconds > 0L) " • ${Formatters.formatDuration(item.etaSeconds)}" else ""
                        Text(
                            text = "${item.progressPercent}% • ${Formatters.formatSpeed(item.speedBytesPerSec)}$etaText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = if (status == DownloadStatus.PAUSED) onResume else onPause,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_pause_resume_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (status == DownloadStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (status == DownloadStatus.PAUSED) strings.resume else strings.pause)
                    }
                }

                DownloadStatus.COMPLETED -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.downloadSuccess,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.savedIn} ${item.folderName} • ${Formatters.formatBytes(item.fileSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play, tint = EmeraldSuccess)
                            }
                        }
                    }
                }

                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = CrimsonError)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.errorMessage ?: if (strings.isArabic) "تم إلغاء أو تعذر التحميل" else "Download stopped or failed",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRetry) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.retry)
                            }
                        }
                    }
                }
            }
        }
    }
}
