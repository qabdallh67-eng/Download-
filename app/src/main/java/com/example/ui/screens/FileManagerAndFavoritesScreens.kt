package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.data.model.MediaDownloadType
import com.example.ui.components.GlowCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.PlatformIconBadge
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.viewmodel.FileCategory
import com.example.ui.viewmodel.FileSortOrder

@Composable
fun FileManagerScreen(
    strings: AppStrings,
    category: FileCategory,
    searchQuery: String,
    sortOrder: FileSortOrder,
    folderDisplayName: String,
    videos: List<DownloadEntity>,
    audioFiles: List<DownloadEntity>,
    favorites: List<DownloadEntity>,
    recent: List<DownloadEntity>,
    onCategoryChange: (FileCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (FileSortOrder) -> Unit,
    onFolderSelected: (android.net.Uri?, String) -> Unit,
    onPlay: (DownloadEntity, List<DownloadEntity>) -> Unit,
    onToggleFavorite: (DownloadEntity) -> Unit,
    onRename: (DownloadEntity, String) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var detailsTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<DownloadEntity?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Selected Folder"
            onFolderSelected(treeUri, name)
        }
    }

    val rawList = when (category) {
        FileCategory.VIDEOS -> videos
        FileCategory.AUDIO -> audioFiles
        FileCategory.FAVORITES -> favorites
        FileCategory.RECENT -> recent
    }

    val displayed = remember(rawList, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) rawList else {
            rawList.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        when (sortOrder) {
            FileSortOrder.NEWEST -> filtered.sortedByDescending { it.completedAt ?: it.createdAt }
            FileSortOrder.NAME -> filtered.sortedBy { it.title.lowercase() }
            FileSortOrder.SIZE -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.navFiles,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.sortNewest) },
                            onClick = { onSortChange(FileSortOrder.NEWEST); sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortName) },
                            onClick = { onSortChange(FileSortOrder.NAME); sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortSize) },
                            onClick = { onSortChange(FileSortOrder.SIZE); sortMenuOpen = false }
                        )
                    }
                }
                IconButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = strings.selectFolder, tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        // Folder Chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ElectricBlue.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { folderPickerLauncher.launch(null) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${strings.selectFolder}: $folderDisplayName",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(strings.searchFilesHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Category Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = category == FileCategory.VIDEOS,
                    onClick = { onCategoryChange(FileCategory.VIDEOS) },
                    label = { Text("${strings.catVideos} (${videos.size})") }
                )
            }
            item {
                FilterChip(
                    selected = category == FileCategory.AUDIO,
                    onClick = { onCategoryChange(FileCategory.AUDIO) },
                    label = { Text("${strings.catAudio} (${audioFiles.size})") }
                )
            }
            item {
                FilterChip(
                    selected = category == FileCategory.FAVORITES,
                    onClick = { onCategoryChange(FileCategory.FAVORITES) },
                    label = { Text("${strings.catFavorites} (${favorites.size})") }
                )
            }
            item {
                FilterChip(
                    selected = category == FileCategory.RECENT,
                    onClick = { onCategoryChange(FileCategory.RECENT) },
                    label = { Text("${strings.catRecent} (${recent.size})") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (displayed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (strings.isArabic) "لا توجد ملفات في هذا التصنيف حالياً." else "No files in this category yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayed, key = { it.id }) { file ->
                    MediaFileRowCard(
                        strings = strings,
                        item = file,
                        onPlay = { onPlay(file, displayed) },
                        onToggleFavorite = { onToggleFavorite(file) },
                        onShare = { onShare(file) },
                        onRename = { renameTarget = file },
                        onDetails = { detailsTarget = file },
                        onDelete = { deleteTarget = file }
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.rename) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) onRename(target, draft)
                    renameTarget = null
                }) {
                    Text(if (strings.isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    detailsTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { detailsTarget = null },
            title = { Text(strings.details) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${if (strings.isArabic) "الاسم:" else "Title:"} ${target.title}")
                    Text("${if (strings.isArabic) "المنصة:" else "Platform:"} ${target.platformEnum.displayName(strings.isArabic)}")
                    Text("${if (strings.isArabic) "الجودة:" else "Quality:"} ${target.qualityLabel}")
                    Text("${if (strings.isArabic) "الصيغة:" else "Format:"} ${target.formatExt.uppercase()}")
                    Text("${if (strings.isArabic) "الحجم:" else "Size:"} ${Formatters.formatBytes(target.fileSizeBytes)}")
                    Text("${if (strings.isArabic) "المجلد:" else "Folder:"} ${target.folderName}")
                    Text("${if (strings.isArabic) "التاريخ:" else "Date:"} ${Formatters.formatDate(target.completedAt ?: target.createdAt, strings.isArabic)}")
                    target.localFilePath?.let {
                        Text("${if (strings.isArabic) "المسار:" else "Path:"} $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsTarget = null }) { Text(strings.closeLabel) }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.delete) },
            text = {
                Text(
                    if (strings.isArabic) "هل تريد حذف «${target.title}» من الجهاز؟" else "Delete \"${target.title}\" from device?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) {
                    Text(strings.delete, color = CrimsonError)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun FavoritesScreen(
    strings: AppStrings,
    favorites: List<DownloadEntity>,
    onPlay: (DownloadEntity, List<DownloadEntity>) -> Unit,
    onToggleFavorite: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onRename: (DownloadEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.navFavorites,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (favorites.isNotEmpty()) {
                GradientPrimaryButton(
                    text = if (strings.isArabic) "تشغيل الكل" else "Play All",
                    onClick = { onPlay(favorites.first(), favorites) },
                    icon = { Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.width(150.dp)
                )
            }
        }

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (strings.isArabic) "لا توجد ملفات مفضلة بعد. اضغط على القلب لإضافة أي فيديو أو صوت." else "No favorites yet. Tap the heart icon on any media file.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favorites, key = { it.id }) { item ->
                    MediaFileRowCard(
                        strings = strings,
                        item = item,
                        onPlay = { onPlay(item, favorites) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onShare = { onShare(item) },
                        onRename = {},
                        onDetails = {},
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaFileRowCard(
    strings: AppStrings,
    item: DownloadEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    GlowCard(
        glowBorder = false,
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
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
                    Icon(
                        imageVector = if (item.mediaTypeEnum == MediaDownloadType.AUDIO) Icons.Rounded.MusicNote else Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformIconBadge(platform = item.platformEnum, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${item.formatExt.uppercase()} • ${Formatters.formatBytes(item.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = strings.catFavorites,
                    tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play, tint = MaterialTheme.colorScheme.primary)
            }

            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = strings.details)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        text = { Text(strings.share) },
                        onClick = { menuOpen = false; onShare() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                        text = { Text(strings.rename) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        text = { Text(strings.details) },
                        onClick = { menuOpen = false; onDetails() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = CrimsonError) },
                        text = { Text(strings.delete, color = CrimsonError) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}
