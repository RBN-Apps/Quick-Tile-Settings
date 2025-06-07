package com.rbn.qtsettings.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbn.qtsettings.R
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

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

    // Help Shown
    private val _helpShown = MutableStateFlow(sharedPreferences.getBoolean(KEY_HELP_SHOWN, false))
    val helpShown: StateFlow<Boolean> = _helpShown.asStateFlow()

    init {
        loadDnsHostnames()
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

    fun setUsbEnableAutoRevert(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_ENABLE_AUTO_REVERT, enabled) }
        _usbEnableAutoRevert.value = enabled
    }

    fun setUsbAutoRevertDelaySeconds(delay: Int) {
        sharedPreferences.edit { putInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, delay) }
        _usbAutoRevertDelaySeconds.value = delay
    }

    fun setHelpShown(shown: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_HELP_SHOWN, shown) }
        _helpShown.value = shown
    }

    fun setVpnDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_VPN_DETECTION_ENABLED, enabled) }
        _vpnDetectionEnabled.value = enabled
    }

    fun setVpnDetectionMode(mode: String) {
        sharedPreferences.edit { putString(KEY_VPN_DETECTION_MODE, mode) }
        _vpnDetectionMode.value = mode
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
        }
    }

    fun addCustomDnsHostname(name: String, hostnameValue: String) {
        val newList = _dnsHostnames.value.toMutableList()
        newList.add(
            DnsHostnameEntry(
                name = name,
                hostname = hostnameValue,
                isPredefined = false,
                isSelectedForCycle = true
            )
        )
        _dnsHostnames.value = sortDnsHostnames(newList.toList())
        saveDnsHostnamesInternal()
    }

    fun deleteCustomDnsHostname(id: String) {
        val currentList = _dnsHostnames.value.toMutableList()
        currentList.removeAll { it.id == id && !it.isPredefined }
        _dnsHostnames.value = sortDnsHostnames(currentList.toList())
        saveDnsHostnamesInternal()
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

    fun isUsbAutoRevertEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_ENABLE_AUTO_REVERT, false)

    fun getUsbAutoRevertDelaySeconds(): Int =
        sharedPreferences.getInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, 5)

    fun isVpnDetectionEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_VPN_DETECTION_ENABLED, false)

    fun getVpnDetectionMode(): String =
        sharedPreferences.getString(KEY_VPN_DETECTION_MODE, TILE_ONLY_DETECTION)
            ?: TILE_ONLY_DETECTION

    companion object {
        private const val KEY_DNS_TOGGLE_OFF = "dns_toggle_off"
        private const val KEY_DNS_TOGGLE_AUTO = "dns_toggle_auto"
        private const val KEY_DNS_ENABLE_AUTO_REVERT = "dns_enable_auto_revert"
        private const val KEY_DNS_AUTO_REVERT_DELAY_SECONDS = "dns_auto_revert_delay_seconds"
        private const val KEY_DNS_HOSTNAMES = "dns_hostnames_list_v2"


        private const val KEY_USB_TOGGLE_ENABLE = "usb_toggle_enable"
        private const val KEY_USB_TOGGLE_DISABLE = "usb_toggle_disable"
        private const val KEY_USB_ENABLE_AUTO_REVERT = "usb_enable_auto_revert"
        private const val KEY_USB_AUTO_REVERT_DELAY_SECONDS = "usb_auto_revert_delay_seconds"

        private const val KEY_HELP_SHOWN = "help_shown_v1"

        private const val KEY_VPN_DETECTION_ENABLED = "vpn_detection_enabled"
        private const val KEY_VPN_DETECTION_MODE = "vpn_detection_mode"

        const val KEY_DNS_PREVIOUS_MODE_FOR_REVERT = "dns_previous_mode_for_revert"
        const val KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT = "dns_previous_hostname_for_revert"
        const val KEY_USB_PREVIOUS_STATE_FOR_REVERT = "usb_previous_state_for_revert"


        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
