package com.rbn.qtsettings.feature.settings.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.rbn.qtsettings.data.*
import com.rbn.qtsettings.feature.settings.domain.SettingsBackupAccess
import com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
import com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
import com.rbn.qtsettings.feature.shortcuts.data.ShortcutSettingsStore
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import java.time.Instant

/** Validates the entire document before writing all feature settings in one editor. */
class SettingsBackupStore(
    private val sharedPreferences: SharedPreferences,
    private val dnsStore: DnsSettingsStore,
    private val usbStore: UsbSettingsStore,
    private val automationStore: AutomationSettingsStore,
    private val shortcutStore: ShortcutSettingsStore
) : SettingsBackupAccess {
    private val gson = Gson()

    override fun exportSettingsBackupJson(): String {
        val exportedAtEpochMillis = System.currentTimeMillis()
        return gson.toJson(
            SettingsBackup(
                exportedAtEpochMillis = exportedAtEpochMillis,
                exportedAtIso8601 = Instant.ofEpochMilli(exportedAtEpochMillis).toString(),
                dns = DnsSettingsBackup(
                    toggleOff = dnsStore.dnsToggleOff.value,
                    toggleAuto = dnsStore.dnsToggleAuto.value,
                    hostnames = dnsStore.manualEntries(),
                    sortMode = dnsStore.dnsListSortMode.value.persistedValue,
                    enableAutoRevert = dnsStore.dnsEnableAutoRevert.value,
                    autoRevertDelaySeconds = dnsStore.dnsAutoRevertDelaySeconds.value,
                    requireUnlock = dnsStore.dnsRequireUnlock.value,
                    vpnDetectionEnabled = automationStore.vpnDetectionEnabled.value,
                    vpnDetectionMode = automationStore.vpnDetectionMode.value,
                    networkTypeDetectionEnabled = automationStore.networkTypeDetectionEnabled.value,
                    networkTypeDetectionMode = automationStore.networkTypeDetectionMode.value,
                    dnsStateOnWifi = automationStore.dnsStateOnWifi.value,
                    dnsHostnameOnWifi = automationStore.dnsHostnameOnWifi.value,
                    wifiNetworkRulesEnabled = automationStore.wifiNetworkRulesEnabled.value,
                    wifiNetworkRules = automationStore.wifiNetworkRules.value,
                    knownWifiNetworks = automationStore.knownWifiNetworks.value,
                    dnsStateOnMobile = automationStore.dnsStateOnMobile.value,
                    dnsHostnameOnMobile = automationStore.dnsHostnameOnMobile.value
                ),
                usb = UsbSettingsBackup(
                    toggleEnable = usbStore.state.value.toggleEnable,
                    toggleDisable = usbStore.state.value.toggleDisable,
                    alsoHideDevOptions = usbStore.state.value.alsoHideDevOptions,
                    alsoDisableWirelessDebugging = usbStore.state.value.alsoDisableWirelessDebugging,
                    enableAutoRevert = usbStore.state.value.enableAutoRevert,
                    autoRevertDelaySeconds = usbStore.state.value.autoRevertDelaySeconds,
                    requireUnlock = usbStore.state.value.requireUnlock
                ),
                shortcuts = ShortcutSettingsBackup(
                    enabledShortcutIds = shortcutStore.enabledShortcutIds.value,
                    favoriteShortcutIds = shortcutStore.favoriteShortcutIds.value,
                    allowPinnedShortcutsWhenDisabled = shortcutStore.allowPinnedShortcutsWhenDisabled.value
                )
            )
        )
    }

    override fun restoreSettingsBackupJson(json: String) {
        val backup = try {
            SettingsBackupJsonParser(gson).parse(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup file", e)
        }

        if (backup.schemaVersion != SettingsBackup.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: ${backup.schemaVersion}")
        }

        val validatedBackup = SettingsBackupValidator(shortcutStore.shortcutMaxCount.value).validate(backup)
        val dns = validatedBackup.dns
        val usb = validatedBackup.usb
        val shortcuts = validatedBackup.shortcuts

        sharedPreferences.edit {
            dnsStore.writeTo(this, dns)
            automationStore.writeTo(this, dns)
            shortcutStore.writeTo(this, shortcuts)

            usbStore.writeTo(this, UsbSettings(
                usb.toggleEnable, usb.toggleDisable, usb.alsoHideDevOptions,
                usb.alsoDisableWirelessDebugging, usb.enableAutoRevert,
                usb.autoRevertDelaySeconds, usb.requireUnlock
            ))
        }

        usbStore.refresh()
        dnsStore.refresh()
        automationStore.refresh()

        shortcutStore.restore(shortcuts)
    }

}
