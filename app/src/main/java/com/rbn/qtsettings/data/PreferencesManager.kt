package com.rbn.qtsettings.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbn.qtsettings.R
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.ShortcutUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)

    // DNS Settings
    private val _dnsToggleOff =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true))
    val dnsToggleOff: StateFlow<Boolean> = _dnsToggleOff.asStateFlow()

    private val _dnsToggleAuto =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true))
    val dnsToggleAuto: StateFlow<Boolean> = _dnsToggleAuto.asStateFlow()

    private val hostnameEntryListType = object : TypeToken<List<DnsHostnameEntry>>() {}.type
    private val _dnsHostnames = MutableStateFlow<List<DnsHostnameEntry>>(emptyList())
    val dnsHostnames: StateFlow<List<DnsHostnameEntry>> = _dnsHostnames.asStateFlow()

    private val _dnsEnableAutoRevert =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_ENABLE_AUTO_REVERT, false))
    val dnsEnableAutoRevert: StateFlow<Boolean> = _dnsEnableAutoRevert.asStateFlow()

    private val _dnsAutoRevertDelaySeconds =
        MutableStateFlow(sharedPreferences.getInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, 5))
    val dnsAutoRevertDelaySeconds: StateFlow<Int> = _dnsAutoRevertDelaySeconds.asStateFlow()


    // USB Debugging Settings
    private val _usbToggleEnable =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_TOGGLE_ENABLE, true))
    val usbToggleEnable: StateFlow<Boolean> = _usbToggleEnable.asStateFlow()

    private val _usbToggleDisable =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_TOGGLE_DISABLE, true))
    val usbToggleDisable: StateFlow<Boolean> = _usbToggleDisable.asStateFlow()

    private val _usbAlsoHideDevOptions =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, false))
    val usbAlsoHideDevOptions: StateFlow<Boolean> = _usbAlsoHideDevOptions.asStateFlow()

    private val _usbAlsoDisableWirelessDebugging =
        MutableStateFlow(
            sharedPreferences.getBoolean(
                KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING,
                false
            )
        )
    val usbAlsoDisableWirelessDebugging: StateFlow<Boolean> =
        _usbAlsoDisableWirelessDebugging.asStateFlow()

    private val _usbEnableAutoRevert =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_ENABLE_AUTO_REVERT, false))
    val usbEnableAutoRevert: StateFlow<Boolean> = _usbEnableAutoRevert.asStateFlow()
    private val _usbAutoRevertDelaySeconds =
        MutableStateFlow(sharedPreferences.getInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, 5))
    val usbAutoRevertDelaySeconds: StateFlow<Int> = _usbAutoRevertDelaySeconds.asStateFlow()

    // VPN Detection Settings
    private val _vpnDetectionEnabled =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_VPN_DETECTION_ENABLED, false))
    val vpnDetectionEnabled: StateFlow<Boolean> = _vpnDetectionEnabled.asStateFlow()

    private val _vpnDetectionMode =
        MutableStateFlow(
            sharedPreferences.getString(KEY_VPN_DETECTION_MODE, TILE_ONLY_DETECTION)
                ?: TILE_ONLY_DETECTION
        )
    val vpnDetectionMode: StateFlow<String> = _vpnDetectionMode.asStateFlow()

    // Network Type Detection Settings
    private val _networkTypeDetectionEnabled =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, false))
    val networkTypeDetectionEnabled: StateFlow<Boolean> = _networkTypeDetectionEnabled.asStateFlow()

    private val _networkTypeDetectionMode =
        MutableStateFlow(
            sharedPreferences.getString(KEY_NETWORK_TYPE_DETECTION_MODE, TILE_ONLY_DETECTION)
                ?: TILE_ONLY_DETECTION
        )
    val networkTypeDetectionMode: StateFlow<String> = _networkTypeDetectionMode.asStateFlow()

    private val _dnsStateOnWifi =
        MutableStateFlow(
            sharedPreferences.getString(KEY_DNS_STATE_ON_WIFI, Constants.DNS_MODE_OFF)
                ?: Constants.DNS_MODE_OFF
        )
    val dnsStateOnWifi: StateFlow<String> = _dnsStateOnWifi.asStateFlow()

    private val _dnsHostnameOnWifi =
        MutableStateFlow(sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_WIFI, null))
    val dnsHostnameOnWifi: StateFlow<String?> = _dnsHostnameOnWifi.asStateFlow()

    private val _dnsStateOnMobile =
        MutableStateFlow(
            sharedPreferences.getString(KEY_DNS_STATE_ON_MOBILE, Constants.DNS_MODE_AUTO)
                ?: Constants.DNS_MODE_AUTO
        )
    val dnsStateOnMobile: StateFlow<String> = _dnsStateOnMobile.asStateFlow()

    private val _dnsHostnameOnMobile =
        MutableStateFlow(sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_MOBILE, null))
    val dnsHostnameOnMobile: StateFlow<String?> = _dnsHostnameOnMobile.asStateFlow()

    // Security Settings
    private val _dnsRequireUnlock =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_REQUIRE_UNLOCK, false))
    val dnsRequireUnlock: StateFlow<Boolean> = _dnsRequireUnlock.asStateFlow()

    private val _usbRequireUnlock =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_REQUIRE_UNLOCK, false))
    val usbRequireUnlock: StateFlow<Boolean> = _usbRequireUnlock.asStateFlow()

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
        loadDnsHostnames()
        loadEnabledShortcutIds()
        loadFavoriteShortcutIds()
        loadAllowPinnedShortcutsWhenDisabled()
        refreshShortcutConfiguration()
    }


    fun setDnsToggleOff(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_OFF, enabled) }
        _dnsToggleOff.value = enabled
    }

    fun setDnsToggleAuto(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_AUTO, enabled) }
        _dnsToggleAuto.value = enabled
    }

    fun setDnsEnableAutoRevert(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_ENABLE_AUTO_REVERT, enabled) }
        _dnsEnableAutoRevert.value = enabled
    }

    fun setDnsAutoRevertDelaySeconds(delay: Int) {
        sharedPreferences.edit { putInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, delay) }
        _dnsAutoRevertDelaySeconds.value = delay
    }


    fun setUsbToggleEnable(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_TOGGLE_ENABLE, enabled) }
        _usbToggleEnable.value = enabled
    }

    fun setUsbToggleDisable(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_TOGGLE_DISABLE, enabled) }
        _usbToggleDisable.value = enabled
    }

    fun setUsbAlsoHideDevOptions(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, enabled) }
        _usbAlsoHideDevOptions.value = enabled
    }

    fun setUsbAlsoDisableWirelessDebugging(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING, enabled) }
        _usbAlsoDisableWirelessDebugging.value = enabled
    }

    fun setUsbEnableAutoRevert(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_ENABLE_AUTO_REVERT, enabled) }
        _usbEnableAutoRevert.value = enabled
    }

    fun setUsbAutoRevertDelaySeconds(delay: Int) {
        sharedPreferences.edit { putInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, delay) }
        _usbAutoRevertDelaySeconds.value = delay
    }

    fun setDnsRequireUnlock(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_REQUIRE_UNLOCK, enabled) }
        _dnsRequireUnlock.value = enabled
    }

    fun setUsbRequireUnlock(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_REQUIRE_UNLOCK, enabled) }
        _usbRequireUnlock.value = enabled
    }

    fun setHelpShown(shown: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_HELP_SHOWN, shown) }
        _helpShown.value = shown
    }

    fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED, enabled) }
        _allowPinnedShortcutsWhenDisabled.value = enabled
    }

    fun setVpnDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_VPN_DETECTION_ENABLED, enabled) }
        _vpnDetectionEnabled.value = enabled
    }

    fun setVpnDetectionMode(mode: String) {
        sharedPreferences.edit { putString(KEY_VPN_DETECTION_MODE, mode) }
        _vpnDetectionMode.value = mode
    }

    fun setNetworkTypeDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, enabled) }
        _networkTypeDetectionEnabled.value = enabled
    }

    fun setNetworkTypeDetectionMode(mode: String) {
        sharedPreferences.edit { putString(KEY_NETWORK_TYPE_DETECTION_MODE, mode) }
        _networkTypeDetectionMode.value = mode
    }

    fun setDnsStateOnWifi(state: String) {
        sharedPreferences.edit { putString(KEY_DNS_STATE_ON_WIFI, state) }
        _dnsStateOnWifi.value = state
    }

    fun setDnsHostnameOnWifi(hostname: String?) {
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAME_ON_WIFI, hostname) }
        _dnsHostnameOnWifi.value = hostname
    }

    fun setDnsStateOnMobile(state: String) {
        sharedPreferences.edit { putString(KEY_DNS_STATE_ON_MOBILE, state) }
        _dnsStateOnMobile.value = state
    }

    fun setDnsHostnameOnMobile(hostname: String?) {
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAME_ON_MOBILE, hostname) }
        _dnsHostnameOnMobile.value = hostname
    }

    private fun sortDnsHostnames(hostnames: List<DnsHostnameEntry>): List<DnsHostnameEntry> {
        return hostnames.sortedWith(
            compareBy(
                { !it.isPredefined },
                { it.name }
            )
        )
    }

    private fun loadDnsHostnames() {
        val json = sharedPreferences.getString(KEY_DNS_HOSTNAMES, null)
        val storedHostnames = if (json != null) {
            try {
                gson.fromJson<List<DnsHostnameEntry>>(json, hostnameEntryListType)
            } catch (e: Exception) {
                Log.e("PreferencesManager", "Error parsing stored DNS hostnames", e)
                null
            }
        } else null

        if (storedHostnames.isNullOrEmpty()) {
            _dnsHostnames.value = getDefaultDnsHostnames()
            saveDnsHostnamesInternal()
        } else {
            val defaultPredefined = getDefaultDnsHostnames().filter { it.isPredefined }
            val customStored = storedHostnames.filter { !it.isPredefined }
            val finalPredefined = defaultPredefined.map { defaultEntry ->
                val storedPredefined =
                    storedHostnames.find { it.id == defaultEntry.id && it.isPredefined }
                storedPredefined?.copy(
                    name = defaultEntry.name,
                    hostname = defaultEntry.hostname,
                    descriptionResId = defaultEntry.descriptionResId
                ) ?: defaultEntry
            }

            _dnsHostnames.value = sortDnsHostnames(finalPredefined + customStored)
            saveDnsHostnamesInternal()
        }
    }


    private fun saveDnsHostnamesInternal() {
        val json = gson.toJson(_dnsHostnames.value)
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAMES, json) }
    }

    fun updateDnsHostnameEntry(updatedEntry: DnsHostnameEntry) {
        val currentList = _dnsHostnames.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedEntry.id }
        if (index != -1) {
            currentList[index] = updatedEntry
            _dnsHostnames.value = sortDnsHostnames(currentList.toList())
            saveDnsHostnamesInternal()
            synchronizeEnabledShortcutsWithAvailableShortcuts()
            synchronizeFavoriteShortcutsWithAvailableShortcuts()
            updateExposedShortcuts()
        }
    }

    fun addCustomDnsHostname(name: String, hostnameValue: String) {
        val newEntry = DnsHostnameEntry(
            name = name,
            hostname = hostnameValue,
            isPredefined = false,
            isSelectedForCycle = true
        )
        val newList = _dnsHostnames.value.toMutableList()
        newList.add(newEntry)
        _dnsHostnames.value = sortDnsHostnames(newList.toList())
        saveDnsHostnamesInternal()
        synchronizeEnabledShortcutsWithAvailableShortcuts()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    fun deleteCustomDnsHostname(id: String) {
        val currentList = _dnsHostnames.value.toMutableList()
        currentList.removeAll { it.id == id && !it.isPredefined }
        _dnsHostnames.value = sortDnsHostnames(currentList.toList())
        saveDnsHostnamesInternal()
        synchronizeEnabledShortcutsWithAvailableShortcuts()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean): Boolean {
        val availableShortcutIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
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
        val availableShortcutIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
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
        val availableIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)

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
        val availableIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
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
        val availableIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
        _enabledShortcutIds.value = _enabledShortcutIds.value.intersect(availableIds)
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
    }

    private fun synchronizeFavoriteShortcutsWithAvailableShortcuts() {
        val availableIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
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
            hostnames = _dnsHostnames.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
        return preferredOrder
            .filter { shortcutIds.contains(it) }
            .take(_shortcutMaxCount.value)
            .toSet()
    }

    private fun normalizeFavoriteIds(shortcutIds: Set<String>): Set<String> {
        val preferredOrder = ShortcutUtils.getOrderedShortcutIds(_dnsHostnames.value)
        val enabledIds = _enabledShortcutIds.value
        return preferredOrder
            .filter { shortcutIds.contains(it) && enabledIds.contains(it) }
            .take(ShortcutUtils.MAX_FAVORITE_SHORTCUTS)
            .toSet()
    }

    private fun updateExposedShortcuts() {
        ShortcutUtils.updateExposedShortcuts(
            context = appContext,
            dnsHostnames = _dnsHostnames.value,
            enabledShortcutIds = _enabledShortcutIds.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
    }

    fun canExecuteShortcutId(shortcutId: String): Boolean {
        val availableIds = ShortcutUtils.getAvailableShortcutIds(_dnsHostnames.value)
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
            _dnsHostnames.value.firstOrNull { it.id == entryId && !it.isPredefined } ?: return null
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

    private fun getDefaultDnsHostnames(): List<DnsHostnameEntry> {
        return listOf(
            DnsHostnameEntry(
                id = "adguard_default",
                name = "AdGuard DNS",
                hostname = "dns.adguard.com",
                isPredefined = true,
                isSelectedForCycle = true,
                descriptionResId = R.string.dns_info_adguard
            ),
            DnsHostnameEntry(
                id = "cloudflare_default",
                name = "Cloudflare (1.1.1.1)",
                hostname = "one.one.one.one",
                isPredefined = true,
                isSelectedForCycle = true,
                descriptionResId = R.string.dns_info_cloudflare
            ),
            DnsHostnameEntry(
                id = "quad9_default",
                name = "Quad9 Security",
                hostname = "dns.quad9.net",
                isPredefined = true,
                isSelectedForCycle = true,
                descriptionResId = R.string.dns_info_quad9
            )
        )
    }

    fun isDnsToggleOffEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true)
    fun isDnsToggleAutoEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true)

    fun getDnsHostnamesSelectedForCycle(): List<DnsHostnameEntry> {
        val json = sharedPreferences.getString(KEY_DNS_HOSTNAMES, null)
        val hostnames = if (json != null) {
            try {
                gson.fromJson(json, hostnameEntryListType)
            } catch (_: Exception) {
                emptyList()
            }
        } else getDefaultDnsHostnames()
        return hostnames.filter { it.isSelectedForCycle }
    }

    fun getAllDnsHostnamesBlocking(): List<DnsHostnameEntry> {
        val json = sharedPreferences.getString(KEY_DNS_HOSTNAMES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, hostnameEntryListType)
            } catch (e: Exception) {
                Log.e(
                    "PreferencesManager",
                    "Error parsing stored DNS hostnames for blocking read",
                    e
                )
                getDefaultDnsHostnames()
            }
        } else {
            getDefaultDnsHostnames()
        }
    }

    fun isDnsAutoRevertEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_DNS_ENABLE_AUTO_REVERT, false)

    fun getDnsAutoRevertDelaySeconds(): Int =
        sharedPreferences.getInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, 5)

    fun isUsbToggleEnableEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_TOGGLE_ENABLE, true)

    fun isUsbToggleDisableEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_TOGGLE_DISABLE, true)

    fun isUsbAlsoHideDevOptionsEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, false)

    fun isUsbAlsoDisableWirelessDebuggingEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING, false)

    fun isUsbAutoRevertEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_ENABLE_AUTO_REVERT, false)

    fun getUsbAutoRevertDelaySeconds(): Int =
        sharedPreferences.getInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, 5)

    fun isDnsRequireUnlockEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_DNS_REQUIRE_UNLOCK, false)

    fun isUsbRequireUnlockEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_REQUIRE_UNLOCK, false)

    fun isVpnDetectionEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_VPN_DETECTION_ENABLED, false)

    fun getVpnDetectionMode(): String =
        sharedPreferences.getString(KEY_VPN_DETECTION_MODE, TILE_ONLY_DETECTION)
            ?: TILE_ONLY_DETECTION

    fun isNetworkTypeDetectionEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, false)

    fun getNetworkTypeDetectionMode(): String =
        sharedPreferences.getString(KEY_NETWORK_TYPE_DETECTION_MODE, TILE_ONLY_DETECTION)
            ?: TILE_ONLY_DETECTION

    fun getDnsStateOnWifi(): String =
        sharedPreferences.getString(KEY_DNS_STATE_ON_WIFI, Constants.DNS_MODE_OFF)
            ?: Constants.DNS_MODE_OFF

    fun getDnsHostnameOnWifi(): String? =
        sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_WIFI, null)

    fun getDnsStateOnMobile(): String =
        sharedPreferences.getString(KEY_DNS_STATE_ON_MOBILE, Constants.DNS_MODE_AUTO)
            ?: Constants.DNS_MODE_AUTO

    fun getDnsHostnameOnMobile(): String? =
        sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_MOBILE, null)

    companion object {
        private const val KEY_DNS_TOGGLE_OFF = "dns_toggle_off"
        private const val KEY_DNS_TOGGLE_AUTO = "dns_toggle_auto"
        private const val KEY_DNS_ENABLE_AUTO_REVERT = "dns_enable_auto_revert"
        private const val KEY_DNS_AUTO_REVERT_DELAY_SECONDS = "dns_auto_revert_delay_seconds"
        private const val KEY_DNS_HOSTNAMES = "dns_hostnames_list_v2"


        private const val KEY_USB_TOGGLE_ENABLE = "usb_toggle_enable"
        private const val KEY_USB_TOGGLE_DISABLE = "usb_toggle_disable"
        private const val KEY_USB_ALSO_HIDE_DEV_OPTIONS = "usb_also_hide_dev_options"
        private const val KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING =
            "usb_also_disable_wireless_debugging"
        private const val KEY_USB_ENABLE_AUTO_REVERT = "usb_enable_auto_revert"
        private const val KEY_USB_AUTO_REVERT_DELAY_SECONDS = "usb_auto_revert_delay_seconds"

        private const val KEY_DNS_REQUIRE_UNLOCK = "dns_require_unlock"
        private const val KEY_USB_REQUIRE_UNLOCK = "usb_require_unlock"

        private const val KEY_HELP_SHOWN = "help_shown_v1"

        private const val KEY_VPN_DETECTION_ENABLED = "vpn_detection_enabled"
        private const val KEY_VPN_DETECTION_MODE = "vpn_detection_mode"

        private const val KEY_NETWORK_TYPE_DETECTION_ENABLED = "network_type_detection_enabled"
        private const val KEY_NETWORK_TYPE_DETECTION_MODE = "network_type_detection_mode"
        private const val KEY_DNS_STATE_ON_WIFI = "dns_state_on_wifi"
        private const val KEY_DNS_HOSTNAME_ON_WIFI = "dns_hostname_on_wifi"
        private const val KEY_DNS_STATE_ON_MOBILE = "dns_state_on_mobile"
        private const val KEY_DNS_HOSTNAME_ON_MOBILE = "dns_hostname_on_mobile"
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
