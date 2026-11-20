package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BrandSplashScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.strings.AppStrings
import com.example.ui.theme.AbdullahTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRich
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SkyCanvasBottom
import com.example.ui.theme.SkyCanvasTop
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object DownloadsRoute
@Serializable object FilesRoute
@Serializable object FavoritesRoute
@Serializable object SettingsRoute
@Serializable object PlayerRoute

private data class TopNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val pendingOpenTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingOpenTab.value = intent?.getStringExtra(EXTRA_OPEN_TAB)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val strings = remember(settings.isArabic) { AppStrings(settings.isArabic) }
            val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
            val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
            val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
            val completedVideos by viewModel.completedVideos.collectAsStateWithLifecycle()
            val completedAudio by viewModel.completedAudio.collectAsStateWithLifecycle()
            val favorites by viewModel.favorites.collectAsStateWithLifecycle()
            val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
            val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
            val fileCategory by viewModel.fileCategory.collectAsStateWithLifecycle()
            val fileSearchQuery by viewModel.fileSearchQuery.collectAsStateWithLifecycle()
            val fileSortOrder by viewModel.fileSortOrder.collectAsStateWithLifecycle()
            val storageUsedBytes by viewModel.storageUsedBytes.collectAsStateWithLifecycle()
            val playerPlaylist by viewModel.activePlayerPlaylist.collectAsStateWithLifecycle()
            val playerIndex by viewModel.activePlayerIndex.collectAsStateWithLifecycle()

            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(1350L)
                showSplash = false
            }

            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* best-effort */ }

            AbdullahTheme(darkTheme = settings.darkMode, isArabic = settings.isArabic) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                val openTab = pendingOpenTab.value
                LaunchedEffect(openTab) {
                    if (openTab == "downloads") {
                        navController.navigate(DownloadsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        pendingOpenTab.value = null
                    }
                }

                val isPlayerRoute = currentDestination?.hasRoute<PlayerRoute>() == true

                val navItems = listOf(
                    TopNavItem(HomeRoute, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
                    TopNavItem(
                        DownloadsRoute,
                        strings.navDownloads,
                        Icons.Filled.Download,
                        Icons.Outlined.Download,
                        badgeCount = activeDownloads.size
                    ),
                    TopNavItem(FilesRoute, strings.navFiles, Icons.Filled.Folder, Icons.Outlined.Folder),
                    TopNavItem(FavoritesRoute, strings.navFavorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
                    TopNavItem(SettingsRoute, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
                )

                fun navigateTab(route: Any) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                val screenBgBrush = if (settings.darkMode) {
                    Brush.verticalGradient(listOf(NavyVoid, Color(0xFF071530)))
                } else {
                    Brush.verticalGradient(listOf(SkyCanvasTop, SkyCanvasBottom))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBgBrush)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp && !isPlayerRoute

                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                if (!useRail && !isPlayerRoute) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        navItems.forEach { item ->
                                            val selected = currentDestination?.hasRoute(item.route::class) == true
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTab(item.route) },
                                                icon = {
                                                    NavIconWithBadge(item, selected)
                                                },
                                                label = {
                                                    Text(
                                                        text = item.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = if (settings.darkMode) GoldBright else ElectricBlue,
                                                    selectedTextColor = if (settings.darkMode) CyanGlow else ElectricBlue,
                                                    indicatorColor = ElectricBlue.copy(alpha = 0.16f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useRail) {
                                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                        navItems.forEach { item ->
                                            val selected = currentDestination?.hasRoute(item.route::class) == true
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { navigateTab(item.route) },
                                                icon = { NavIconWithBadge(item, selected) },
                                                label = { Text(item.label, maxLines = 1) }
                                            )
                                        }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = HomeRoute,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    composable<HomeRoute> {
                                        HomeScreen(
                                            strings = strings,
                                            settings = settings,
                                            urlInput = urlInput,
                                            analysisState = analysisState,
                                            recentCompleted = completedDownloads,
                                            activeDownloads = activeDownloads,
                                            onUrlChange = viewModel::updateUrlInput,
                                            onAnalyzeClick = { viewModel.analyzeUrl(it) },
                                            onDismissPopup = viewModel::dismissAnalysisPopup,
                                            onStartDownload = { res, type, vq, af ->
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    ContextCompat.checkSelfPermission(
                                                        this@MainActivity,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                                viewModel.enqueueDownload(res, type, vq, af)
                                            },
                                            onPauseResume = { item ->
                                                if (item.status == "PAUSED") viewModel.resumeDownload(item.id)
                                                else viewModel.pauseDownload(item.id)
                                            },
                                            onPlayMedia = { item ->
                                                viewModel.openPlayer(item, completedDownloads)
                                                navController.navigate(PlayerRoute)
                                            },
                                            onOpenDownloadsTab = { navigateTab(DownloadsRoute) },
                                            onOpenSettingsTab = { navigateTab(SettingsRoute) }
                                        )
                                    }

                                    composable<DownloadsRoute> {
                                        DownloadsScreen(
                                            strings = strings,
                                            allDownloads = allDownloads,
                                            onPause = viewModel::pauseDownload,
                                            onResume = viewModel::resumeDownload,
                                            onCancel = viewModel::cancelDownload,
                                            onRetry = viewModel::retryDownload,
                                            onPlay = { item ->
                                                viewModel.openPlayer(item, completedDownloads)
                                                navController.navigate(PlayerRoute)
                                            },
                                            onClearHistory = viewModel::clearFinishedHistory
                                        )
                                    }

                                    composable<FilesRoute> {
                                        FileManagerScreen(
                                            strings = strings,
                                            category = fileCategory,
                                            searchQuery = fileSearchQuery,
                                            sortOrder = fileSortOrder,
                                            folderDisplayName = settings.downloadFolderDisplayName,
                                            videos = completedVideos,
                                            audioFiles = completedAudio,
                                            favorites = favorites,
                                            recent = completedDownloads,
                                            onCategoryChange = viewModel::setFileCategory,
                                            onSearchChange = viewModel::setFileSearchQuery,
                                            onSortChange = viewModel::setFileSortOrder,
                                            onFolderSelected = viewModel::setDownloadFolder,
                                            onPlay = { item, list ->
                                                viewModel.openPlayer(item, list)
                                                navController.navigate(PlayerRoute)
                                            },
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onRename = viewModel::renameFile,
                                            onDelete = viewModel::deleteDownload,
                                            onShare = { item ->
                                                viewModel.buildShareFileIntent(item)?.let { shareIntent ->
                                                    startActivity(Intent.createChooser(shareIntent, strings.share))
                                                }
                                            }
                                        )
                                    }

                                    composable<FavoritesRoute> {
                                        FavoritesScreen(
                                            strings = strings,
                                            favorites = favorites,
                                            onPlay = { item, list ->
                                                viewModel.openPlayer(item, list)
                                                navController.navigate(PlayerRoute)
                                            },
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onShare = { item ->
                                                viewModel.buildShareFileIntent(item)?.let { shareIntent ->
                                                    startActivity(Intent.createChooser(shareIntent, strings.share))
                                                }
                                            },
                                            onDelete = viewModel::deleteDownload,
                                            onRename = viewModel::renameFile
                                        )
                                    }

                                    composable<SettingsRoute> {
                                        SettingsScreen(
                                            strings = strings,
                                            settings = settings,
                                            storageUsedBytes = storageUsedBytes,
                                            onSetArabic = viewModel::setArabic,
                                            onSetDarkMode = viewModel::setDarkMode,
                                            onSetDefaultVideoQuality = viewModel::setDefaultVideoQuality,
                                            onSetDefaultAudioFormat = viewModel::setDefaultAudioFormat,
                                            onSetDownloadFolder = viewModel::setDownloadFolder,
                                            onSetWifiOnly = viewModel::setWifiOnly,
                                            onSetNotifications = viewModel::setNotificationsEnabled,
                                            onClearTempCache = { viewModel.clearTempFiles {} }
                                        )
                                    }

                                    composable<PlayerRoute> {
                                        PlayerScreen(
                                            strings = strings,
                                            playlist = playerPlaylist,
                                            initialIndex = playerIndex,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut()
                    ) {
                        BrandSplashScreen(
                            isArabic = settings.isArabic,
                            darkMode = settings.darkMode
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenTab.value = intent.getStringExtra(EXTRA_OPEN_TAB)
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }
}

@Composable
private fun NavIconWithBadge(item: TopNavItem, selected: Boolean) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    if (item.badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(item.badgeCount.toString()) } }) {
            Icon(icon, contentDescription = item.label)
        }
    } else {
        Icon(icon, contentDescription = item.label)
    }
}
