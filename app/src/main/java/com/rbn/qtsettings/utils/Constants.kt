package com.rbn.qtsettings.utils

object Constants {
    const val PRIVATE_DNS_MODE = "private_dns_mode"
    const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier" // Hostname for DNS_MODE_ON
    const val ADB_ENABLED = "adb_enabled" // USB Debugging (0 or 1)
    const val DEVELOPMENT_SETTINGS_ENABLED =
        "development_settings_enabled" // Developer Options (0 or 1)

    const val DNS_MODE_OFF = "off"
    const val DNS_MODE_AUTO = "opportunistic"
    const val DNS_MODE_ON = "hostname"

    const val TILE_ONLY_DETECTION = "tile_only"
    const val BACKGROUND_DETECTION = "background"

    // Network type constants
    const val NETWORK_TYPE_WIFI = "wifi"
    const val NETWORK_TYPE_MOBILE = "mobile"
    const val NETWORK_TYPE_NONE = "none"
}