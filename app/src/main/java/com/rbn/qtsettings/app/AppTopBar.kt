package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppRailTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    destination: AppDestination,
    selectedTarget: AppRailTarget,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNavigation: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        // The tile roots share one destination, so the selected area names the page.
        title = {
            Text(stringResource(
                if (destination == AppDestination.TileSettings) selectedTarget.title else destination.title
            ))
        },
        navigationIcon = {
            Row {
                if (onOpenNavigation != null) IconButton(onClick = onOpenNavigation) {
                    Icon(Icons.Default.Menu, stringResource(R.string.navigation_open))
                }
                if (destination != AppDestination.TileSettings) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back_desc))
                }
            }
        }
    )
}

private val AppDestination.title: Int get() = when (this) {
    AppDestination.ShortcutSettings -> R.string.shortcut_settings_title
    AppDestination.AppSettings -> R.string.app_settings_title
    AppDestination.Permissions -> R.string.menu_permission_help
    AppDestination.AdbInstructions -> R.string.adb_instructions_title
    AppDestination.About -> R.string.menu_about
    AppDestination.UsbTile -> R.string.usb_menu_tile
    AppDestination.UsbActions -> R.string.usb_menu_actions
    AppDestination.DnsProviders -> R.string.dns_menu_providers
    AppDestination.DnsAutomation -> R.string.dns_menu_automation
    AppDestination.DnsWifiTarget -> R.string.automation_wifi
    AppDestination.DnsMobileTarget -> R.string.automation_mobile
    AppDestination.WifiRules -> R.string.setting_wifi_network_rules_title
    AppDestination.WifiRule -> R.string.wifi_rule_title
    AppDestination.DnsRevert -> R.string.usb_menu_revert
    AppDestination.UsbRevert -> R.string.usb_menu_revert
    AppDestination.TileSettings -> R.string.app_name
}
