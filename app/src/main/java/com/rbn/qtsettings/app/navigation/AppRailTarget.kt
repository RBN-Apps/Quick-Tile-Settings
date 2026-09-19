package com.rbn.qtsettings.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.ui.graphics.vector.ImageVector
import com.rbn.qtsettings.R

/** Chrome metadata only; stable route identities remain in AppRoute. */
enum class AppRailGroup(@param:StringRes val title: Int) {
    Tiles(R.string.navigation_group_tiles), App(R.string.navigation_group_app), Help(R.string.navigation_group_help)
}

enum class AppRailTarget(
    val group: AppRailGroup,
    @param:StringRes val title: Int,
    @param:StringRes val shortTitle: Int,
    val icon: ImageVector,
    val area: AppArea? = null,
    val destination: AppDestination? = null
) {
    Dns(AppRailGroup.Tiles, R.string.tab_title_dns, R.string.navigation_dns_short, Icons.Default.Dns, AppArea.Dns),
    Usb(AppRailGroup.Tiles, R.string.tab_title_usb, R.string.navigation_usb_short, Icons.Default.Usb, AppArea.Usb),
    Shortcuts(AppRailGroup.App, R.string.shortcut_settings_title, R.string.navigation_shortcuts_short,
        Icons.Default.AppShortcut, destination = AppDestination.ShortcutSettings),
    Settings(AppRailGroup.App, R.string.menu_settings, R.string.menu_settings,
        Icons.Default.Settings, destination = AppDestination.AppSettings),
    Permissions(AppRailGroup.Help, R.string.menu_permission_help, R.string.navigation_permissions_short,
        Icons.Default.AdminPanelSettings, destination = AppDestination.Permissions),
    About(AppRailGroup.Help, R.string.menu_about, R.string.navigation_about_short,
        Icons.Default.Info, destination = AppDestination.About);

    val testTag: String get() = if (area != null) "navigation_area_${area.name}" else "navigation_target_$name"

    companion object {
        fun forDestination(destination: AppDestination, area: AppArea): AppRailTarget = when (destination) {
            AppDestination.ShortcutSettings, AppDestination.ShortcutHelp -> Shortcuts
            AppDestination.AppSettings -> Settings
            AppDestination.About -> About
            AppDestination.Permissions, AppDestination.AdbInstructions -> Permissions
            AppDestination.DnsProviders, AppDestination.DnsAutomation, AppDestination.DnsWifiTarget,
            AppDestination.DnsMobileTarget, AppDestination.WifiRules, AppDestination.WifiRule, AppDestination.DnsRevert -> Dns
            AppDestination.UsbTile, AppDestination.UsbActions, AppDestination.UsbRevert -> Usb
            AppDestination.TileSettings -> if (area == AppArea.Dns) Dns else Usb
        }
    }
}
