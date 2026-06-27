package com.rbn.qtsettings.utils

object Constants {
    const val PRIVATE_DNS_MODE = "private_dns_mode"
    const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier" // Hostname for DNS_MODE_ON
    const val ADB_ENABLED = "adb_enabled" // USB Debugging (0 or 1)
    const val ADB_WIFI_ENABLED = "adb_wifi_enabled" // Wireless Debugging (0 or 1)
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

    // Shortcut actions
    const val ACTION_DNS_OFF = "com.rbn.qtsettings.action.DNS_OFF"
    const val ACTION_DNS_AUTO = "com.rbn.qtsettings.action.DNS_AUTO"
    const val ACTION_DNS_ADGUARD = "com.rbn.qtsettings.action.DNS_ADGUARD"
    const val ACTION_DNS_CLOUDFLARE = "com.rbn.qtsettings.action.DNS_CLOUDFLARE"
    const val ACTION_DNS_QUAD9 = "com.rbn.qtsettings.action.DNS_QUAD9"
    const val ACTION_DNS_CUSTOM = "com.rbn.qtsettings.action.DNS_CUSTOM"
    const val ACTION_USB_ON = "com.rbn.qtsettings.action.USB_ON"
    const val ACTION_USB_OFF = "com.rbn.qtsettings.action.USB_OFF"

    // Shortcut intent extras
    const val EXTRA_DNS_ENTRY_ID = "extra_dns_entry_id"
}
