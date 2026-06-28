package com.rbn.qtsettings.data

import com.google.gson.JsonObject

data class SettingsBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long = 0,
    val exportedAtIso8601: String = "",
    val dns: DnsSettingsBackup? = null,
    val usb: UsbSettingsBackup? = null,
    val shortcuts: ShortcutSettingsBackup? = null,
    val features: Map<String, JsonObject> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class DnsSettingsBackup(
    val toggleOff: Boolean = true,
    val toggleAuto: Boolean = true,
    val hostnames: List<DnsHostnameEntry> = emptyList(),
    val enableAutoRevert: Boolean = false,
    val autoRevertDelaySeconds: Int = 5,
    val requireUnlock: Boolean = false,
    val vpnDetectionEnabled: Boolean = false,
    val vpnDetectionMode: String = "tile_only",
    val networkTypeDetectionEnabled: Boolean = false,
    val networkTypeDetectionMode: String = "tile_only",
    val dnsStateOnWifi: String = "off",
    val dnsHostnameOnWifi: String? = null,
    val dnsStateOnMobile: String = "opportunistic",
    val dnsHostnameOnMobile: String? = null
)

data class UsbSettingsBackup(
    val toggleEnable: Boolean = true,
    val toggleDisable: Boolean = true,
    val alsoHideDevOptions: Boolean = false,
    val alsoDisableWirelessDebugging: Boolean = false,
    val enableAutoRevert: Boolean = false,
    val autoRevertDelaySeconds: Int = 5,
    val requireUnlock: Boolean = false
)

data class ShortcutSettingsBackup(
    val enabledShortcutIds: Set<String> = emptySet(),
    val favoriteShortcutIds: Set<String> = emptySet(),
    val allowPinnedShortcutsWhenDisabled: Boolean = false
)
