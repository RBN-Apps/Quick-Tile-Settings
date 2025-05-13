package com.rbn.qtsettings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)

    // DNS Settings
    private val _dnsToggleOff =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true))
    val dnsToggleOff: StateFlow<Boolean> = _dnsToggleOff.asStateFlow()

    private val _dnsToggleAuto =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true))
    val dnsToggleAuto: StateFlow<Boolean> = _dnsToggleAuto.asStateFlow()

    private val _dnsToggleOn =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_ON, true))
    val dnsToggleOn: StateFlow<Boolean> = _dnsToggleOn.asStateFlow()

    private val _dnsHostname = MutableStateFlow(
        sharedPreferences.getString(
            KEY_DNS_HOSTNAME,
            "1dot1dot1dot1.cloudflare-dns.com"
        ) ?: "1dot1dot1dot1.cloudflare-dns.com"
    )
    val dnsHostname: StateFlow<String> = _dnsHostname.asStateFlow()

    // USB Debugging Settings
    private val _usbToggleEnable =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_TOGGLE_ENABLE, true))
    val usbToggleEnable: StateFlow<Boolean> = _usbToggleEnable.asStateFlow()

    private val _usbToggleDisable =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_USB_TOGGLE_DISABLE, true))
    val usbToggleDisable: StateFlow<Boolean> = _usbToggleDisable.asStateFlow()

    // Help Shown
    private val _helpShown = MutableStateFlow(sharedPreferences.getBoolean(KEY_HELP_SHOWN, false))
    val helpShown: StateFlow<Boolean> = _helpShown.asStateFlow()


    fun setDnsToggleOff(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_OFF, enabled) }
        _dnsToggleOff.value = enabled
    }

    fun setDnsToggleAuto(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_AUTO, enabled) }
        _dnsToggleAuto.value = enabled
    }

    fun setDnsToggleOn(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_ON, enabled) }
        _dnsToggleOn.value = enabled
    }

    fun setDnsHostname(hostname: String) {
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAME, hostname) }
        _dnsHostname.value = hostname
    }

    fun setUsbToggleEnable(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_TOGGLE_ENABLE, enabled) }
        _usbToggleEnable.value = enabled
    }

    fun setUsbToggleDisable(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_USB_TOGGLE_DISABLE, enabled) }
        _usbToggleDisable.value = enabled
    }

    fun setHelpShown(shown: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_HELP_SHOWN, shown) }
        _helpShown.value = shown
    }

    fun isDnsToggleOffEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true)
    fun isDnsToggleAutoEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true)
    fun isDnsToggleOnEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_ON, true)
    fun getDnsHostname(): String =
        sharedPreferences.getString(KEY_DNS_HOSTNAME, "1dot1dot1dot1.cloudflare-dns.com")
            ?: "1dot1dot1dot1.cloudflare-dns.com"

    fun isUsbToggleEnableEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_TOGGLE_ENABLE, true)

    fun isUsbToggleDisableEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_USB_TOGGLE_DISABLE, true)


    companion object {
        private const val KEY_DNS_TOGGLE_OFF = "dns_toggle_off"
        private const val KEY_DNS_TOGGLE_AUTO = "dns_toggle_auto"
        private const val KEY_DNS_TOGGLE_ON = "dns_toggle_on"
        private const val KEY_DNS_HOSTNAME = "dns_hostname"

        private const val KEY_USB_TOGGLE_ENABLE = "usb_toggle_enable"
        private const val KEY_USB_TOGGLE_DISABLE = "usb_toggle_disable"

        private const val KEY_HELP_SHOWN = "help_shown_v1"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}