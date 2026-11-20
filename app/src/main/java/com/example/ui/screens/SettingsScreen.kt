package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.R
import com.example.data.local.UserSettings
import com.example.ui.components.CrownPlayLogo
import com.example.ui.components.GlowCard
import com.example.ui.components.PortraitAvatar
import com.example.ui.strings.AppStrings
import com.example.ui.strings.Formatters
import com.example.ui.theme.ElectricBlue

@Composable
fun SettingsScreen(
    strings: AppStrings,
    settings: UserSettings,
    storageUsedBytes: Long,
    onSetArabic: (Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetDefaultVideoQuality: (String) -> Unit,
    onSetDefaultAudioFormat: (String) -> Unit,
    onSetDownloadFolder: (android.net.Uri?, String) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onClearTempCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var qualityDropdown by remember { mutableStateOf(false) }
    var audioDropdown by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Custom Folder"
            onSetDownloadFolder(uri, name)
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotifications(granted)
    }

    val qualities = listOf(
        "8K – 4320p",
        "4K – 2160p",
        "2K – 1440p",
        "Full HD – 1080p",
        "HD – 720p",
        "SD – 480p"
    )
    val audioFormats = listOf("MP3", "M4A")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.navSettings,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Personal Branding Banner
        item {
            GlowCard(onClick = { showAboutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PortraitAvatar(size = 66.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appNameAr,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = strings.appNameEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.aboutDeveloper,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    CrownPlayLogo(size = 38.dp)
                }
            }
        }

        // Language & Theme
        item {
            GlowCard(glowBorder = false, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.languageSetting, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (settings.isArabic) "العربية (RTL)" else "English (LTR)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onSetArabic(true) },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (settings.isArabic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("العربية")
                            }
                            OutlinedButton(
                                onClick = { onSetArabic(false) },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (!settings.isArabic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("EN")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.themeSetting, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (settings.darkMode) strings.darkModeLabel else strings.lightModeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.darkMode, onCheckedChange = onSetDarkMode)
                    }
                }
            }
        }

        // Download Defaults & Network
        item {
            GlowCard(glowBorder = false, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { qualityDropdown = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.defaultVideoQuality, style = MaterialTheme.typography.titleSmall)
                            Text(settings.defaultVideoQuality, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = qualityDropdown, onDismissRequest = { qualityDropdown = false }) {
                            qualities.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q) },
                                    onClick = { onSetDefaultVideoQuality(q); qualityDropdown = false }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { audioDropdown = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.defaultAudioFormat, style = MaterialTheme.typography.titleSmall)
                            Text(settings.defaultAudioFormat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        DropdownMenu(expanded = audioDropdown, onDismissRequest = { audioDropdown = false }) {
                            audioFormats.forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt) },
                                    onClick = { onSetDefaultAudioFormat(fmt); audioDropdown = false }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { folderPicker.launch(null) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.downloadFolder, style = MaterialTheme.typography.titleSmall)
                            Text(settings.downloadFolderDisplayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.wifiOnly, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(checked = settings.wifiOnly, onCheckedChange = onSetWifiOnly)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.notifications, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetNotifications(wantEnable)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Storage Management
        item {
            GlowCard(glowBorder = false, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.storageManagement, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${if (strings.isArabic) "المساحة المستخدمة:" else "Used storage:"} ${Formatters.formatBytes(storageUsedBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onClearTempCache()
                            Toast.makeText(
                                context,
                                if (strings.isArabic) "تم تنظيف الملفات المؤقتة" else "Temporary files cleaned",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clearTempCache)
                    }
                }
            }
        }

        // Privacy Policy & About
        item {
            GlowCard(glowBorder = false, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.privacyPolicy, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicy) },
            text = {
                Text(
                    if (strings.isArabic)
                        "يحترم تطبيق عبدالله أبو قاعود خصوصيتك بالكامل:\n\n• لا يتم جمع أي بيانات شخصية أو تتبع خفي.\n• جميع التنزيلات والملفات تُحفظ محلياً على جهازك فقط.\n• لا يتم رفع أي فيديوهات خاصة إلى أي خادم خارجي.\n• يحترم التطبيق حقوق النشر وسياسات المنصات ولا يتجاوز التشفير أو الحماية."
                    else
                        "Abdullah Abu Qaoud Downloader respects your privacy:\n\n• No personal data collection or hidden tracking.\n• All downloads are stored strictly locally on your device.\n• Private user videos are never uploaded anywhere.\n• Respects copyright, DRM, and official platform access rules."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text(strings.closeLabel) }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutDeveloper) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.abdullah_portrait_full),
                        contentDescription = "Abdullah Abu Qaoud",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "عبدالله أبو قاعود",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Abdullah Abu Qaoud",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (strings.isArabic)
                            "تطبيق احترافي متكامل لتحميل وإدارة وتشغيل الفيديو والصوت بجودة عالية مع نافذة المشاركة السريعة الفورية."
                        else
                            "Complete professional video & audio downloader, manager, and Media3 player with instant Quick Share Popup.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(strings.closeLabel) }
            }
        )
    }
}
