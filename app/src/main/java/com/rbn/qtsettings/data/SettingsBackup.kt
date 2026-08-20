package com.rbn.qtsettings.data

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class SettingsBackup(
    @field:SerializedName("schemaVersion")
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @field:SerializedName("exportedAtEpochMillis")
    val exportedAtEpochMillis: Long = 0,
    @field:SerializedName("exportedAtIso8601")
    val exportedAtIso8601: String = "",
    @field:SerializedName("dns")
    val dns: DnsSettingsBackup? = null,
    @field:SerializedName("usb")
    val usb: UsbSettingsBackup? = null,
    @field:SerializedName("shortcuts")
    val shortcuts: ShortcutSettingsBackup? = null,
    @field:SerializedName("features")
    val features: Map<String, JsonObject> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class DnsSettingsBackup(
    @field:SerializedName("toggleOff")
    val toggleOff: Boolean = true,
    @field:SerializedName("toggleAuto")
    val toggleAuto: Boolean = true,
    @field:SerializedName("hostnames")
    val hostnames: List<DnsHostnameEntry> = emptyList(),
    @field:SerializedName("sortMode")
    val sortMode: String = DnsListSortMode.ALPHABETICAL.persistedValue,
    @field:SerializedName("enableAutoRevert")
    val enableAutoRevert: Boolean = false,
    @field:SerializedName("autoRevertDelaySeconds")
    val autoRevertDelaySeconds: Int = 5,
    @field:SerializedName("requireUnlock")
    val requireUnlock: Boolean = false,
    @field:SerializedName("vpnDetectionEnabled")
    val vpnDetectionEnabled: Boolean = false,
    @field:SerializedName("vpnDetectionMode")
    val vpnDetectionMode: String = "tile_only",
    @field:SerializedName("networkTypeDetectionEnabled")
    val networkTypeDetectionEnabled: Boolean = false,
    @field:SerializedName("networkTypeDetectionMode")
    val networkTypeDetectionMode: String = "tile_only",
    @field:SerializedName("dnsStateOnWifi")
    val dnsStateOnWifi: String = "off",
    @field:SerializedName("dnsHostnameOnWifi")
    val dnsHostnameOnWifi: String? = null,
    @field:SerializedName("dnsStateOnMobile")
    val dnsStateOnMobile: String = "opportunistic",
    @field:SerializedName("dnsHostnameOnMobile")
    val dnsHostnameOnMobile: String? = null
)

data class UsbSettingsBackup(
    @field:SerializedName("toggleEnable")
    val toggleEnable: Boolean = true,
    @field:SerializedName("toggleDisable")
    val toggleDisable: Boolean = true,
    @field:SerializedName("alsoHideDevOptions")
    val alsoHideDevOptions: Boolean = false,
    @field:SerializedName("alsoDisableWirelessDebugging")
    val alsoDisableWirelessDebugging: Boolean = false,
    @field:SerializedName("enableAutoRevert")
    val enableAutoRevert: Boolean = false,
    @field:SerializedName("autoRevertDelaySeconds")
    val autoRevertDelaySeconds: Int = 5,
    @field:SerializedName("requireUnlock")
    val requireUnlock: Boolean = false
)

data class ShortcutSettingsBackup(
    @field:SerializedName("enabledShortcutIds")
    val enabledShortcutIds: Set<String> = emptySet(),
    @field:SerializedName("favoriteShortcutIds")
    val favoriteShortcutIds: Set<String> = emptySet(),
    @field:SerializedName("allowPinnedShortcutsWhenDisabled")
    val allowPinnedShortcutsWhenDisabled: Boolean = false
)
