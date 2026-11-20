package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.local.DownloadEntity
import com.example.data.model.MediaDownloadType
import com.example.ui.components.CrownPlayLogo
import com.example.ui.components.GlowCard
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.RoyalPurple
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PlayerScreen(
    strings: AppStrings,
    playlist: List<DownloadEntity>,
    initialIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var currentIndex by remember(playlist, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0)))
    }
    var isFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var speedMenuOpen by remember { mutableStateOf(false) }

    var volumeFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var brightnessFraction by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.75f
        )
    }

    val currentItem = playlist.getOrNull(currentIndex)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Load playlist into ExoPlayer
    LaunchedEffect(playlist) {
        if (playlist.isNotEmpty()) {
            val mediaItems = playlist.mapNotNull { item ->
                val uri: Uri? = when {
                    !item.localFilePath.isNullOrBlank() && File(item.localFilePath).exists() ->
                        Uri.fromFile(File(item.localFilePath))
                    !item.mediaStoreUri.isNullOrBlank() -> Uri.parse(item.mediaStoreUri)
                    item.resolvedMediaUrl.startsWith("http") -> Uri.parse(item.resolvedMediaUrl)
                    else -> null
                }
                uri?.let {
                    MediaItem.Builder()
                        .setUri(it)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build())
                        .build()
                }
            }
            if (mediaItems.isNotEmpty()) {
                exoPlayer.setMediaItems(mediaItems, currentIndex.coerceIn(0, mediaItems.lastIndex), 0L)
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = exoPlayer.currentMediaItemIndex
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(350L)
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    val isAudio = currentItem?.mediaTypeEnum == MediaDownloadType.AUDIO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyVoid)
    ) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.closeLabel,
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem?.title ?: strings.appNameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentItem?.qualityLabel.orEmpty()} • ${currentItem?.formatExt?.uppercase().orEmpty()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBright
                    )
                }
                Box {
                    IconButton(onClick = { speedMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Speed, contentDescription = "Speed", tint = CyanGlow)
                    }
                    DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x") },
                                onClick = {
                                    playbackSpeed = spd
                                    exoPlayer.playbackParameters = PlaybackParameters(spd)
                                    speedMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Video Surface or Audio Artwork Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!isAudio) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(RoyalPurple.copy(alpha = 0.45f), NavyDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CrownPlayLogo(size = 84.dp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = GoldBright)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = currentItem?.formatExt?.uppercase() ?: "AUDIO",
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
        }

        // Transport Controls + Seek Bar + Volume & Brightness
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Slider(
                value = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f,
                onValueChange = { frac ->
                    if (durationMs > 0L) {
                        exoPlayer.seekTo((frac * durationMs).toLong())
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = CyanGlow,
                    inactiveTrackColor = NavyCard
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(Formatters.formatMillis(positionMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text("${playbackSpeed}x", style = MaterialTheme.typography.labelSmall, color = GoldBright)
                Text(Formatters.formatMillis(durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                }
                IconButton(
                    onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color.White)
                }
                Surface(
                    shape = CircleShape,
                    color = ElectricBlue,
                    border = BorderStroke(1.5.dp, CyanGlow),
                    modifier = Modifier
                        .size(60.dp)
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color.White)
                }
                IconButton(
                    onClick = { if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                }
            }

            if (!isFullscreen) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Volume", tint = CyanGlow, modifier = Modifier.size(20.dp))
                        Slider(
                            value = volumeFraction,
                            onValueChange = { v ->
                                volumeFraction = v
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (v * maxVolume).toInt().coerceIn(0, maxVolume),
                                    0
                                )
                            },
                            colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                        )
                    }
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Brightness6, contentDescription = "Brightness", tint = GoldBright, modifier = Modifier.size(20.dp))
                        Slider(
                            value = brightnessFraction,
                            onValueChange = { b ->
                                brightnessFraction = b
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = b.coerceIn(0.05f, 1f)
                                    win.attributes = lp
                                }
                            },
                            colors = SliderDefaults.colors(thumbColor = GoldBright, activeTrackColor = GoldBright)
                        )
                    }
                }
            }
        }

        // Playlist Queue
        if (!isFullscreen && playlist.size > 1) {
            Text(
                text = if (strings.isArabic) "قائمة التشغيل (${playlist.size})" else "Playlist (${playlist.size})",
                style = MaterialTheme.typography.titleSmall,
                color = GoldBright,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(playlist) { index, item ->
                    val active = index == currentIndex
                    GlowCard(
                        glowBorder = active,
                        onClick = {
                            currentIndex = index
                            exoPlayer.seekToDefaultPosition(index)
                            exoPlayer.play()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) GoldBright else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (active) CyanGlow else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
