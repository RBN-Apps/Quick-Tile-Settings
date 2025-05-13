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

    fun isDnsToggleOffEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true)
    fun isDnsToggleAutoEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true)
    fun isDnsToggleOnEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_ON, true)
    fun getDnsHostname(): String =
        sharedPreferences.getString(KEY_DNS_HOSTNAME, "1dot1dot1dot1.cloudflare-dns.com")
            ?: "1dot1dot1dot1.cloudflare-dns.com"

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


    companion object {
        private const val KEY_DNS_TOGGLE_OFF = "dns_toggle_off"
        private const val KEY_DNS_TOGGLE_AUTO = "dns_toggle_auto"
        private const val KEY_DNS_TOGGLE_ON = "dns_toggle_on"
        private const val KEY_DNS_HOSTNAME = "dns_hostname"
        private const val KEY_DNS_ENABLE_AUTO_REVERT = "dns_enable_auto_revert"
        private const val KEY_DNS_AUTO_REVERT_DELAY_SECONDS = "dns_auto_revert_delay_seconds"


        private const val KEY_USB_TOGGLE_ENABLE = "usb_toggle_enable"
        private const val KEY_USB_TOGGLE_DISABLE = "usb_toggle_disable"
        private const val KEY_USB_ENABLE_AUTO_REVERT = "usb_enable_auto_revert"
        private const val KEY_USB_AUTO_REVERT_DELAY_SECONDS = "usb_auto_revert_delay_seconds"

        private const val KEY_HELP_SHOWN = "help_shown_v1"

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