package com.rbn.qtsettings.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenAdbSettings: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val tilesUpdatedMessage = stringResource(R.string.toast_settings_saved_tiles_updated)
    val commandCopiedMessage = stringResource(R.string.toast_command_copied)
    val showPermissionGrantDialog = remember { mutableStateOf(false) }
    val showAboutDialog = remember { mutableStateOf(false) }
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


    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tabTitles = listOf(
        stringResource(R.string.tab_title_dns),
        stringResource(R.string.tab_title_usb)
    )
    val pagerState = rememberPagerState(
        initialPage = viewModel.initialTab.collectAsState().value,
        pageCount = { tabTitles.size }
    )

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        if (!hasWriteSecureSettings) {
                            showPermissionGrantDialog.value = true
                            viewModel.checkSystemStates(context)
                        } else {
                            showAboutDialog.value = true
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_button_desc)
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            if (!hasWriteSecureSettings) {
                PermissionWarningCard(onGrantPermissionClick = {
                    showPermissionGrantDialog.value = true
                    viewModel.checkSystemStates(context)
                })
            }

            SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
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
                Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)) {
                    when (page) {
                        0 -> DnsSettingsCard(viewModel = viewModel)
                        1 -> UsbDebuggingSettingsCard(
                            viewModel = viewModel,
                            isDevOptionsEnabled = isDevOptionsEnabled
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(tilesUpdatedMessage)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(
                        ButtonDefaults.IconSize
                    )
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.button_save_apply_settings))
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
                    Toast.makeText(
                        context,
                        commandCopiedMessage,
                        Toast.LENGTH_SHORT
                    )
                        .show()
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

    // DNS Hostname Add/Edit Dialog
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

    // Confirm Delete Hostname Dialog
    val hostnamePendingDeletion by viewModel.hostnamePendingDeletion.collectAsState()
    hostnamePendingDeletion?.let { entry ->
        ConfirmDeleteDialog(
            hostnameEntry = entry,
            onDismiss = { viewModel.setHostnamePendingDeletion(null) }) {
            viewModel.deleteCustomDnsHostname(entry.id)
        }
    }

    // About Dialog
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

    // Notification Permission Settings Dialog
    val showNotificationSettingsDialog by viewModel.showNotificationSettingsDialog.collectAsState()
    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            onDismiss = { viewModel.clearNotificationSettingsDialog() }
        )
    }
}

@Composable
fun PermissionWarningCard(onGrantPermissionClick: () -> Unit) {
    Card(
        modifier = Modifier
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
