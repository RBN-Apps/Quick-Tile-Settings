package com.rbn.qtsettings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.utils.ShortcutUtils
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
    // Compatibility facade for platform callers, shortcuts and backup; audit removal in #119/#121/#123.
    val dnsStore = DnsSettingsStore(sharedPreferences)
    val dnsToggleOff get() = dnsStore.dnsToggleOff
    val dnsToggleAuto get() = dnsStore.dnsToggleAuto
    val dnsListSortMode get() = dnsStore.dnsListSortMode
    val dnsHostnames get() = dnsStore.dnsHostnames
    val dnsEnableAutoRevert get() = dnsStore.dnsEnableAutoRevert
    val dnsAutoRevertDelaySeconds get() = dnsStore.dnsAutoRevertDelaySeconds
    val dnsRequireUnlock get() = dnsStore.dnsRequireUnlock

    val usbStore = UsbSettingsStore(sharedPreferences)

    val automationStore = com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore(sharedPreferences)
    val vpnDetectionEnabled get() = automationStore.vpnDetectionEnabled
    val vpnDetectionMode get() = automationStore.vpnDetectionMode
    val networkTypeDetectionEnabled get() = automationStore.networkTypeDetectionEnabled
    val networkTypeDetectionMode get() = automationStore.networkTypeDetectionMode
    val dnsStateOnWifi get() = automationStore.dnsStateOnWifi
    val dnsHostnameOnWifi get() = automationStore.dnsHostnameOnWifi
    val wifiNetworkRulesEnabled get() = automationStore.wifiNetworkRulesEnabled
    val wifiNetworkRules get() = automationStore.wifiNetworkRules
    val knownWifiNetworks get() = automationStore.knownWifiNetworks
    val dnsStateOnMobile get() = automationStore.dnsStateOnMobile
    val dnsHostnameOnMobile get() = automationStore.dnsHostnameOnMobile

    // Security Settings
    // Help Shown
    private val _helpShown = MutableStateFlow(sharedPreferences.getBoolean(KEY_HELP_SHOWN, false))
    val helpShown: StateFlow<Boolean> = _helpShown.asStateFlow()

    // App Shortcut Settings
    private val _shortcutMaxCount = MutableStateFlow(ShortcutUtils.getMaxShortcutCount(appContext))
    val shortcutMaxCount: StateFlow<Int> = _shortcutMaxCount.asStateFlow()

    private val _enabledShortcutIds = MutableStateFlow<Set<String>>(emptySet())
    val enabledShortcutIds: StateFlow<Set<String>> = _enabledShortcutIds.asStateFlow()
    private val _favoriteShortcutIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteShortcutIds: StateFlow<Set<String>> = _favoriteShortcutIds.asStateFlow()
    private val _allowPinnedShortcutsWhenDisabled = MutableStateFlow(false)
    val allowPinnedShortcutsWhenDisabled: StateFlow<Boolean> =
        _allowPinnedShortcutsWhenDisabled.asStateFlow()

    init {
        dnsStore.onProvidersChanged = {
            synchronizeEnabledShortcutsWithAvailableShortcuts()
            synchronizeFavoriteShortcutsWithAvailableShortcuts()
            updateExposedShortcuts()
        }
        loadEnabledShortcutIds()
        loadFavoriteShortcutIds()
        loadAllowPinnedShortcutsWhenDisabled()
        refreshShortcutConfiguration()
    }

    fun setDnsToggleOff(enabled: Boolean) = dnsStore.setDnsToggleOff(enabled)

    fun setDnsToggleAuto(enabled: Boolean) = dnsStore.setDnsToggleAuto(enabled)

    fun setDnsListSortMode(mode: DnsListSortMode) = dnsStore.setDnsListSortMode(mode)

    fun setDnsEnableAutoRevert(enabled: Boolean) = dnsStore.setDnsEnableAutoRevert(enabled)

    fun setDnsAutoRevertDelaySeconds(delay: Int) = dnsStore.setDnsAutoRevertDelaySeconds(delay)

    fun setUsbToggleEnable(value: Boolean) = usbStore.setToggleEnable(value)

    fun setUsbToggleDisable(value: Boolean) = usbStore.setToggleDisable(value)

    fun setUsbAlsoHideDevOptions(value: Boolean) = usbStore.setAlsoHideDevOptions(value)

    fun setUsbAlsoDisableWirelessDebugging(value: Boolean) = usbStore.setAlsoDisableWirelessDebugging(value)

    fun setUsbEnableAutoRevert(value: Boolean) = usbStore.setEnableAutoRevert(value)

    fun setUsbAutoRevertDelaySeconds(value: Int) = usbStore.setAutoRevertDelaySeconds(value)

    fun setDnsRequireUnlock(enabled: Boolean) = dnsStore.setDnsRequireUnlock(enabled)

    fun setUsbRequireUnlock(value: Boolean) = usbStore.setRequireUnlock(value)

    fun setHelpShown(shown: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_HELP_SHOWN, shown) }
        _helpShown.value = shown
    }

    fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED, enabled) }
        _allowPinnedShortcutsWhenDisabled.value = enabled
    }

    fun setVpnDetectionEnabled(enabled: Boolean): Unit = automationStore.setVpnDetectionEnabled(enabled)
    fun setVpnDetectionMode(mode: String): Unit = automationStore.setVpnDetectionMode(mode)
    fun setNetworkTypeDetectionEnabled(enabled: Boolean): Unit = automationStore.setNetworkTypeDetectionEnabled(enabled)
    fun setNetworkTypeDetectionMode(mode: String): Unit = automationStore.setNetworkTypeDetectionMode(mode)
    fun setDnsStateOnWifi(state: String): Unit = automationStore.setDnsStateOnWifi(state)
    fun setDnsHostnameOnWifi(hostname: String?): Unit = automationStore.setDnsHostnameOnWifi(hostname)
    fun setWifiNetworkRulesEnabled(enabled: Boolean): Unit = automationStore.setWifiNetworkRulesEnabled(enabled)
    fun addWifiNetworkRule(rule: WifiDnsRule): Boolean = automationStore.addWifiNetworkRule(rule)
    fun updateWifiNetworkRule(rule: WifiDnsRule): Boolean = automationStore.updateWifiNetworkRule(rule)
    fun deleteWifiNetworkRule(ruleId: String): Unit = automationStore.deleteWifiNetworkRule(ruleId)
    fun recordKnownWifiNetwork(
        identity: WifiNetworkIdentity,
        seenAtEpochMillis: Long = System.currentTimeMillis()
    ): Unit = automationStore.recordKnownWifiNetwork(identity, seenAtEpochMillis)
    fun setDnsStateOnMobile(state: String): Unit = automationStore.setDnsStateOnMobile(state)
    fun setDnsHostnameOnMobile(hostname: String?): Unit = automationStore.setDnsHostnameOnMobile(hostname)
    fun isVpnDetectionEnabled(): Boolean = automationStore.isVpnDetectionEnabled()
    fun getVpnDetectionMode(): String = automationStore.getVpnDetectionMode()
    fun isNetworkTypeDetectionEnabled(): Boolean = automationStore.isNetworkTypeDetectionEnabled()
    fun getNetworkTypeDetectionMode(): String = automationStore.getNetworkTypeDetectionMode()
    fun getDnsStateOnWifi(): String = automationStore.getDnsStateOnWifi()
    fun getDnsHostnameOnWifi(): String? = automationStore.getDnsHostnameOnWifi()
    fun areWifiNetworkRulesEnabled(): Boolean = automationStore.areWifiNetworkRulesEnabled()
    fun getWifiNetworkRules(): List<WifiDnsRule> = automationStore.getWifiNetworkRules()
    fun getKnownWifiNetworks(): List<KnownWifiNetwork> = automationStore.getKnownWifiNetworks()
    fun getDnsStateOnMobile(): String = automationStore.getDnsStateOnMobile()
    fun getDnsHostnameOnMobile(): String? = automationStore.getDnsHostnameOnMobile()

    fun exportSettingsBackupJson(): String {
        val exportedAtEpochMillis = System.currentTimeMillis()
        return gson.toJson(
            SettingsBackup(
                exportedAtEpochMillis = exportedAtEpochMillis,
                exportedAtIso8601 = Instant.ofEpochMilli(exportedAtEpochMillis).toString(),
                dns = DnsSettingsBackup(
                    toggleOff = dnsToggleOff.value,
                    toggleAuto = dnsToggleAuto.value,
                    hostnames = dnsStore.manualEntries(),
                    sortMode = dnsListSortMode.value.persistedValue,
                    enableAutoRevert = dnsEnableAutoRevert.value,
                    autoRevertDelaySeconds = dnsAutoRevertDelaySeconds.value,
                    requireUnlock = dnsRequireUnlock.value,
                    vpnDetectionEnabled = vpnDetectionEnabled.value,
                    vpnDetectionMode = vpnDetectionMode.value,
                    networkTypeDetectionEnabled = networkTypeDetectionEnabled.value,
                    networkTypeDetectionMode = networkTypeDetectionMode.value,
                    dnsStateOnWifi = dnsStateOnWifi.value,
                    dnsHostnameOnWifi = dnsHostnameOnWifi.value,
                    wifiNetworkRulesEnabled = wifiNetworkRulesEnabled.value,
                    wifiNetworkRules = wifiNetworkRules.value,
                    knownWifiNetworks = knownWifiNetworks.value,
                    dnsStateOnMobile = dnsStateOnMobile.value,
                    dnsHostnameOnMobile = dnsHostnameOnMobile.value
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
                    enabledShortcutIds = _enabledShortcutIds.value,
                    favoriteShortcutIds = _favoriteShortcutIds.value,
                    allowPinnedShortcutsWhenDisabled = _allowPinnedShortcutsWhenDisabled.value
                )
            )
        )
    }

    fun restoreSettingsBackupJson(json: String) {
        val backup = try {
            SettingsBackupJsonParser(gson).parse(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup file", e)
        }

        if (backup.schemaVersion != SettingsBackup.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: ${backup.schemaVersion}")
        }

        val validatedBackup = SettingsBackupValidator(_shortcutMaxCount.value).validate(backup)
        val dns = validatedBackup.dns
        val usb = validatedBackup.usb
        val shortcuts = validatedBackup.shortcuts

        sharedPreferences.edit {
            dnsStore.writeTo(this, dns)
            automationStore.writeTo(this, dns)

            usbStore.writeTo(this, UsbSettings(
                usb.toggleEnable, usb.toggleDisable, usb.alsoHideDevOptions,
                usb.alsoDisableWirelessDebugging, usb.enableAutoRevert,
                usb.autoRevertDelaySeconds, usb.requireUnlock
            ))

            putBoolean(
                KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED,
                shortcuts.allowPinnedShortcutsWhenDisabled
            )
        }

        usbStore.refresh()
        dnsStore.refresh()
        automationStore.refresh()

        _enabledShortcutIds.value = ShortcutUtils.migrateLegacyShortcutIds(
            shortcuts.enabledShortcutIds
        )
        _favoriteShortcutIds.value = ShortcutUtils.migrateLegacyShortcutIds(
            shortcuts.favoriteShortcutIds
        )
        _allowPinnedShortcutsWhenDisabled.value = shortcuts.allowPinnedShortcutsWhenDisabled

        synchronizeEnabledShortcutsWithAvailableShortcuts()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    /**
     * Reads the persisted DNS hostname list, upgrading entries that were written by a minified
     * release before [DnsHostnameEntry] had stable Gson field names. Without that upgrade the
     * obfuscated keys match no field, Gson hands back all-null entries and
     * [DnsHostnamePolicy.reconcile] drops every custom hostname the user added.
     */

    fun updateDnsHostnameEntry(updatedEntry: DnsHostnameEntry) = dnsStore.updateDnsHostnameEntry(updatedEntry)

    fun addCustomDnsHostname(name: String, hostnameValue: String) = dnsStore.addCustomDnsHostname(name, hostnameValue)

    fun deleteCustomDnsHostname(id: String) = dnsStore.deleteCustomDnsHostname(id)

    fun reorderDnsHostnames(orderedIds: List<String>): Boolean = dnsStore.reorderDnsHostnames(orderedIds)

    fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean): Boolean {
        val availableShortcutIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        if (!availableShortcutIds.contains(shortcutId)) {
            return false
        }

        if (enabled) {
            if (_enabledShortcutIds.value.contains(shortcutId)) {
                return true
            }
            if (_enabledShortcutIds.value.size >= _shortcutMaxCount.value) {
                return false
            }
            _enabledShortcutIds.value = _enabledShortcutIds.value + shortcutId
        } else {
            _enabledShortcutIds.value = _enabledShortcutIds.value - shortcutId
            _favoriteShortcutIds.value = _favoriteShortcutIds.value - shortcutId
            saveFavoriteShortcutIdsInternal()
        }

        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
        return true
    }

    fun setShortcutFavorite(shortcutId: String, favorite: Boolean): Boolean {
        val availableShortcutIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        if (!availableShortcutIds.contains(shortcutId)) {
            return false
        }

        if (favorite) {
            if (!_enabledShortcutIds.value.contains(shortcutId)) {
                return false
            }
            if (_favoriteShortcutIds.value.contains(shortcutId)) {
                return true
            }
            if (_favoriteShortcutIds.value.size >= ShortcutUtils.MAX_FAVORITE_SHORTCUTS) {
                return false
            }
            _favoriteShortcutIds.value = _favoriteShortcutIds.value + shortcutId
        } else {
            if (!_favoriteShortcutIds.value.contains(shortcutId)) {
                return true
            }
            _favoriteShortcutIds.value = _favoriteShortcutIds.value - shortcutId
        }

        _favoriteShortcutIds.value = normalizeFavoriteIds(_favoriteShortcutIds.value)
        saveFavoriteShortcutIdsInternal()
        updateExposedShortcuts()
        return true
    }

    fun refreshShortcutConfiguration() {
        _shortcutMaxCount.value = ShortcutUtils.getMaxShortcutCount(appContext)
        synchronizeEnabledShortcutsWithAvailableShortcuts()
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    private fun loadEnabledShortcutIds() {
        val storedIds = sharedPreferences.getStringSet(KEY_ENABLED_SHORTCUT_IDS, null)?.toSet()
        val migratedStoredIds = storedIds?.let { ShortcutUtils.migrateLegacyShortcutIds(it) }
        val availableIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)

        _enabledShortcutIds.value = if (migratedStoredIds == null) {
            emptySet()
        } else {
            migratedStoredIds.intersect(availableIds)
        }
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
    }

    private fun loadFavoriteShortcutIds() {
        val storedFavoriteIds =
            sharedPreferences.getStringSet(KEY_FAVORITE_SHORTCUT_IDS, null)?.toSet()
        val availableIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        _favoriteShortcutIds.value = normalizeFavoriteIds(
            storedFavoriteIds
                ?.intersect(availableIds)
                ?.intersect(_enabledShortcutIds.value)
                ?: emptySet()
        )
        saveFavoriteShortcutIdsInternal()
    }

    private fun loadAllowPinnedShortcutsWhenDisabled() {
        _allowPinnedShortcutsWhenDisabled.value = sharedPreferences.getBoolean(
            KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED,
            false
        )
    }

    private fun synchronizeEnabledShortcutsWithAvailableShortcuts() {
        val availableIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        _enabledShortcutIds.value = _enabledShortcutIds.value.intersect(availableIds)
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
    }

    private fun synchronizeFavoriteShortcutsWithAvailableShortcuts() {
        val availableIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        _favoriteShortcutIds.value = normalizeFavoriteIds(
            _favoriteShortcutIds.value
                .intersect(availableIds)
                .intersect(_enabledShortcutIds.value)
        )
        saveFavoriteShortcutIdsInternal()
    }

    private fun limitShortcutIdsToMaxCount(shortcutIds: Set<String>): Set<String> {
        if (shortcutIds.size <= _shortcutMaxCount.value) return shortcutIds

        val preferredOrder = ShortcutUtils.getOrderedShortcutIds(
            hostnames = dnsHostnames.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
        return preferredOrder
            .filter { shortcutIds.contains(it) }
            .take(_shortcutMaxCount.value)
            .toSet()
    }

    private fun normalizeFavoriteIds(shortcutIds: Set<String>): Set<String> {
        val preferredOrder = ShortcutUtils.getOrderedShortcutIds(dnsHostnames.value)
        val enabledIds = _enabledShortcutIds.value
        return preferredOrder
            .filter { shortcutIds.contains(it) && enabledIds.contains(it) }
            .take(ShortcutUtils.MAX_FAVORITE_SHORTCUTS)
            .toSet()
    }

    private fun updateExposedShortcuts() {
        ShortcutUtils.updateExposedShortcuts(
            context = appContext,
            dnsHostnames = dnsHostnames.value,
            enabledShortcutIds = _enabledShortcutIds.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
    }

    fun canExecuteShortcutId(shortcutId: String): Boolean {
        val availableIds = ShortcutUtils.getAvailableShortcutIds(dnsHostnames.value)
        if (!availableIds.contains(shortcutId)) {
            return false
        }
        return _enabledShortcutIds.value.contains(shortcutId) || _allowPinnedShortcutsWhenDisabled.value
    }

    fun resolveCustomDnsShortcutEntry(entryId: String?): DnsHostnameEntry? {
        if (entryId.isNullOrBlank()) {
            return null
        }

        val entry =
            dnsHostnames.value.firstOrNull { it.id == entryId && !it.isPredefined } ?: return null
        val shortcutId = ShortcutUtils.getShortcutIdForDnsEntry(entry)
        return entry.takeIf { canExecuteShortcutId(shortcutId) }
    }

    private fun saveEnabledShortcutIdsInternal() {
        sharedPreferences.edit { putStringSet(KEY_ENABLED_SHORTCUT_IDS, _enabledShortcutIds.value) }
    }

    private fun saveFavoriteShortcutIdsInternal() {
        sharedPreferences.edit {
            putStringSet(
                KEY_FAVORITE_SHORTCUT_IDS,
                _favoriteShortcutIds.value
            )
        }
    }

    fun isDnsToggleOffEnabled(): Boolean = dnsStore.read().toggleOff
    fun isDnsToggleAutoEnabled(): Boolean = dnsStore.read().toggleAuto

    fun getDnsListSortModeBlocking(): DnsListSortMode = dnsStore.getDnsListSortModeBlocking()

    fun getDnsHostnamesSelectedForCycle(): List<DnsHostnameEntry> = dnsStore.getDnsHostnamesSelectedForCycle()

    fun getAllDnsHostnamesBlocking(): List<DnsHostnameEntry> = dnsStore.getAllDnsHostnamesBlocking()

    fun isDnsAutoRevertEnabled(): Boolean = dnsStore.read().enableAutoRevert

    fun getDnsAutoRevertDelaySeconds(): Int = dnsStore.read().autoRevertDelaySeconds

    fun isUsbToggleEnableEnabled(): Boolean = usbStore.read().toggleEnable

    fun isUsbToggleDisableEnabled(): Boolean = usbStore.read().toggleDisable

    fun isUsbAlsoHideDevOptionsEnabled(): Boolean = usbStore.read().alsoHideDevOptions

    fun isUsbAlsoDisableWirelessDebuggingEnabled(): Boolean = usbStore.read().alsoDisableWirelessDebugging

    fun isUsbAutoRevertEnabled(): Boolean = usbStore.read().enableAutoRevert

    fun getUsbAutoRevertDelaySeconds(): Int = usbStore.read().autoRevertDelaySeconds

    fun isDnsRequireUnlockEnabled(): Boolean = dnsStore.read().requireUnlock

    fun isUsbRequireUnlockEnabled(): Boolean = usbStore.read().requireUnlock

    companion object {

        private const val KEY_HELP_SHOWN = "help_shown_v1"

        private const val KEY_ENABLED_SHORTCUT_IDS = "enabled_shortcut_ids_v1"
        private const val KEY_FAVORITE_SHORTCUT_IDS = "favorite_shortcut_ids_v1"
        private const val KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED =
            "allow_pinned_shortcuts_when_disabled_v1"
        const val KEY_DNS_PREVIOUS_MODE_FOR_REVERT = "dns_previous_mode_for_revert"
        const val KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT = "dns_previous_hostname_for_revert"
        const val KEY_USB_PREVIOUS_STATE_FOR_REVERT = "usb_previous_state_for_revert"
        const val KEY_DEV_OPTIONS_PREVIOUS_STATE_FOR_REVERT =
            "dev_options_previous_state_for_revert"
        const val KEY_WIRELESS_DEBUGGING_PREVIOUS_STATE_FOR_REVERT =
            "wireless_debugging_previous_state_for_revert"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
