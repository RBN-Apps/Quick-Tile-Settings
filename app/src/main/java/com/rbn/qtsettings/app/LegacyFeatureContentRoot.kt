package com.rbn.qtsettings.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.ui.composables.settings.AppSettingsScreen
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutSettingsScreen
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Only the roots bridge to existing VM-coupled features. Remove per #113–#122. */
@Composable
fun LegacyFeatureContentRoot(
    route: AppRoute,
    viewModel: MainViewModel
) {
    when (route) {
        is AppRoute.Shortcuts -> ShortcutSettingsScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
        )
        is AppRoute.Settings -> AppSettingsScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()
        )
        AppRoute.Usb, AppRoute.UsbTile, AppRoute.UsbActions, AppRoute.UsbRevert -> error("USB has its own feature root")
        AppRoute.Dns -> error("DNS has its own feature root")
    }
}
