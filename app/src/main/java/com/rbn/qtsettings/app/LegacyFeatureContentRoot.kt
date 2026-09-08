package com.rbn.qtsettings.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.LegacyTilesScreen
import com.rbn.qtsettings.ui.composables.dns.DnsSettingsCard
import com.rbn.qtsettings.ui.composables.usb.UsbDebuggingSettingsCard
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutSettingsScreen
import com.rbn.qtsettings.ui.composables.settings.AppSettingsScreen
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Only the roots bridge to existing VM-coupled features. Remove per #113–#122. */
@Composable
fun LegacyFeatureContentRoot(
    destination: AppDestination,
    viewModel: MainViewModel,
    pagerState: PagerState,
    onShowPermissionHelp: () -> Unit
) {
    when (destination) {
        AppDestination.ShortcutSettings -> ShortcutSettingsScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
        )
        AppDestination.AppSettings -> AppSettingsScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()
        )
        AppDestination.TileSettings -> {
            val hasPermission by viewModel.hasWriteSecureSettings.collectAsStateWithLifecycle()
            val developerOptionsEnabled by viewModel.developerOptionsEnabled.collectAsStateWithLifecycle()
            LegacyTilesScreen(pagerState, hasPermission, onShowPermissionHelp) { area ->
                Column(Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                    when (area) {
                        AppArea.Dns -> DnsSettingsCard(viewModel)
                        AppArea.Usb -> UsbDebuggingSettingsCard(viewModel, developerOptionsEnabled)
                    }
                }
            }
        }
    }
}
