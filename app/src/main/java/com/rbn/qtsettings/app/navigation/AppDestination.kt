package com.rbn.qtsettings.app.navigation

import kotlinx.serialization.Serializable

/** Chrome labels/callbacks only; navigation is owned by AppNavigation's typed stacks. */
enum class AppDestination { TileSettings, ShortcutSettings, ShortcutHelp, AppSettings, About, Licenses, Permissions, AdbInstructions, DnsProviders, DnsAutomation, DnsWifiTarget, DnsMobileTarget, WifiRules, WifiRule, DnsRevert, UsbTile, UsbActions, UsbRevert }

@Serializable
enum class AppArea { Dns, Usb }
