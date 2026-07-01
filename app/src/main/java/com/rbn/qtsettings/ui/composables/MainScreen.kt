package com.rbn.qtsettings.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class MainScreenDestination {
    TileSettings,
    ShortcutSettings,
    AppSettings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenAdbSettings: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val commandCopiedMessage = stringResource(R.string.toast_command_copied)
    val showPermissionGrantDialog = remember { mutableStateOf(false) }
    val showAboutDialog = remember { mutableStateOf(false) }
    val currentScreen = rememberSaveable { mutableStateOf(MainScreenDestination.TileSettings) }
    val showOverflowMenu = remember { mutableStateOf(false) }
    val hasWriteSecureSettings by viewModel.hasWriteSecureSettings.collectAsState()

    val isDevOptionsEnabled by remember {
        mutableStateOf(PermissionUtils.isDeveloperOptionsEnabled(context))
    }

    val helpShown by viewModel.helpShown.collectAsState()
    LaunchedEffect(hasWriteSecureSettings, helpShown) {
        if (!hasWriteSecureSettings && !helpShown) {
            showPermissionGrantDialog.value = true
            viewModel.checkSystemStates(context)
        }
    }

    val inSubScreen = currentScreen.value != MainScreenDestination.TileSettings
    BackHandler(enabled = inSubScreen) {
        currentScreen.value = MainScreenDestination.TileSettings
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.backupStatusMessage.collect { message ->
            if (message != null) {
                viewModel.clearBackupStatusMessage()
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val tabTitles = listOf(
        stringResource(R.string.tab_title_dns),
        stringResource(R.string.tab_title_usb)
    )
    val initialTab by viewModel.initialTab.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { tabTitles.size }
    )

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
//        containerColor = MaterialTheme.colorScheme.surface,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (inSubScreen) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen.value) {
                                MainScreenDestination.ShortcutSettings ->
                                    stringResource(R.string.shortcut_settings_title)

                                MainScreenDestination.AppSettings ->
                                    stringResource(R.string.app_settings_title)

                                MainScreenDestination.TileSettings ->
                                    stringResource(R.string.app_name)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                currentScreen.value = MainScreenDestination.TileSettings
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_desc)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(
                            onClick = {
                                currentScreen.value = MainScreenDestination.ShortcutSettings
                            }
                        ) {
                            Icon(
                                Icons.Filled.AppShortcut,
                                contentDescription = stringResource(R.string.shortcut_settings_open_desc)
                            )
                        }
                        IconButton(onClick = { showOverflowMenu.value = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.app_menu_desc)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu.value,
                            onDismissRequest = { showOverflowMenu.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_settings)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu.value = false
                                    currentScreen.value = MainScreenDestination.AppSettings
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_permission_help)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.AdminPanelSettings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu.value = false
                                    showPermissionGrantDialog.value = true
                                    viewModel.checkSystemStates(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_about)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu.value = false
                                    showAboutDialog.value = true
                                }
                            )
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen.value,
            transitionSpec = {
                if (targetState != MainScreenDestination.TileSettings) {
                    (slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(260),
                                targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                            ) + fadeOut(animationSpec = tween(200)))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(260),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(animationSpec = tween(200)))
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            label = "screenTransition"
        ) { destination ->
            when (destination) {
                MainScreenDestination.ShortcutSettings -> {
                    ShortcutSettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxSize()
                    )
                }

                MainScreenDestination.AppSettings -> {
                    AppSettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxSize()
                    )
                }

                MainScreenDestination.TileSettings -> {
                    MainTilesContent(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        hasWriteSecureSettings = hasWriteSecureSettings,
                        isDevOptionsEnabled = isDevOptionsEnabled,
                        tabTitles = tabTitles,
                        pagerState = pagerState,
                        onShowPermissionDialog = {
                            showPermissionGrantDialog.value = true
                            viewModel.checkSystemStates(context)
                        }
                    )
                }
            }
        }

        val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
        val appHasShizukuPermission by viewModel.appHasShizukuPermission.collectAsState()
        val isDeviceRooted by viewModel.isDeviceRooted.collectAsState()

        if (showPermissionGrantDialog.value) {
            PermissionGrantDialog(
                onDismissRequest = {
                    showPermissionGrantDialog.value = false
                    if (!hasWriteSecureSettings) {
                        viewModel.setHelpShown(true)
                    }
                },
                onOpenDeveloperOptions = onOpenAdbSettings,
                onCopyToClipboard = { textToCopy ->
                    val clip = ClipData.newPlainText("ADB Command", textToCopy)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, commandCopiedMessage, Toast.LENGTH_SHORT).show()
                },
                onGrantWithShizuku = { viewModel.grantWriteSecureSettingsViaShizuku(context) },
                onGrantWithRoot = { viewModel.grantWriteSecureSettingsViaRoot(context) },
                onRequestShizukuPermission = onRequestShizukuPermission,
                isShizukuAvailable = isShizukuAvailable,
                appHasShizukuPermission = appHasShizukuPermission,
                isDeviceRooted = isDeviceRooted
            )
        }
    }

    val editingHostname by viewModel.editingHostname.collectAsState()
    if (viewModel.showHostnameEditDialog.collectAsState().value) {
        DnsHostnameEditDialog(
            entry = editingHostname,
            onDismiss = { viewModel.dismissHostnameEditDialog() },
            onSave = { id, name, hostVal ->
                if (id == null) {
                    viewModel.addCustomDnsHostname(name, hostVal)
                } else {
                    viewModel.editCustomDnsHostname(id, name, hostVal)
                }
            },
            viewModel = viewModel
        )
    }

    val hostnamePendingDeletion by viewModel.hostnamePendingDeletion.collectAsState()
    hostnamePendingDeletion?.let { entry ->
        ConfirmDeleteDialog(
            hostnameEntry = entry,
            onDismiss = { viewModel.setHostnamePendingDeletion(null) }
        ) {
            viewModel.deleteCustomDnsHostname(entry.id)
        }
    }

    if (showAboutDialog.value) {
        AboutDialog(
            onDismissRequest = { showAboutDialog.value = false },
            onOpenPermissionDialog = {
                showAboutDialog.value = false
                showPermissionGrantDialog.value = true
                viewModel.checkSystemStates(context)
            }
        )
    }

    val showNotificationSettingsDialog by viewModel.showNotificationSettingsDialog.collectAsState()
    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            onDismiss = { viewModel.clearNotificationSettingsDialog() }
        )
    }

    val showNotificationPermissionExplanationDialog by
        viewModel.showNotificationPermissionExplanationDialog.collectAsState()
    val notificationPermissionExplanationFromBackup by
        viewModel.notificationPermissionExplanationFromBackup.collectAsState()
    if (showNotificationPermissionExplanationDialog) {
        NotificationPermissionExplanationDialog(
            fromBackup = notificationPermissionExplanationFromBackup,
            onGrantPermission = { viewModel.requestNotificationPermissionFromExplanation() },
            onDismiss = { viewModel.useTileOnlyDetectionForNotificationFallback() }
        )
    }

    val showNotificationPermissionFallbackDialog by
        viewModel.showNotificationPermissionFallbackDialog.collectAsState()
    if (showNotificationPermissionFallbackDialog) {
        NotificationPermissionFallbackDialog(
            onGrantPermission = { viewModel.requestNotificationPermissionFromExplanation() },
            onUseTileOnly = { viewModel.useTileOnlyDetectionForNotificationFallback() }
        )
    }

    val showNotificationPermissionSettingsDialog by
        viewModel.showNotificationPermissionSettingsDialog.collectAsState()
    if (showNotificationPermissionSettingsDialog) {
        NotificationPermissionSettingsDialog(
            onOpenSettings = { viewModel.openNotificationPermissionSettings() },
            onUseTileOnly = { viewModel.useTileOnlyDetectionForNotificationFallback() }
        )
    }
}

@Composable
private fun MainTilesContent(
    modifier: Modifier,
    viewModel: MainViewModel,
    hasWriteSecureSettings: Boolean,
    isDevOptionsEnabled: Boolean,
    tabTitles: List<String>,
    pagerState: PagerState,
    onShowPermissionDialog: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        if (!hasWriteSecureSettings) {
            PermissionWarningCard(
                onGrantPermissionClick = onShowPermissionDialog,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                when (page) {
                    0 -> DnsSettingsCard(viewModel = viewModel)
                    1 -> UsbDebuggingSettingsCard(
                        viewModel = viewModel,
                        isDevOptionsEnabled = isDevOptionsEnabled
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.warning_permission_missing_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.warning_permission_missing_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGrantPermissionClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    stringResource(R.string.button_show_permission_help),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    QuickTileSettingsTheme {
        val context = LocalContext.current
        val fakePrefs = com.rbn.qtsettings.data.PreferencesManager.getInstance(context)
        MainScreen(
            viewModel = MainViewModel(fakePrefs),
            onOpenAdbSettings = {},
            onRequestShizukuPermission = {}
        )
    }
}
