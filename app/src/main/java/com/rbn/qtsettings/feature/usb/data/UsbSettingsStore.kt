package com.rbn.qtsettings.feature.usb.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.feature.usb.domain.UsbSettingsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uses the existing preference file; restore and synchronous service reads share this owner. */
class UsbSettingsStore(private val preferences: SharedPreferences) : UsbSettingsSource {
    private val mutableState = MutableStateFlow(read())
    override val state = mutableState.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }
    init { preferences.registerOnSharedPreferenceChangeListener(listener) }
    fun refresh() { mutableState.value = read() }
    override fun read() = UsbSettings(
        toggleEnable = preferences.getBoolean(KEY_USB_TOGGLE_ENABLE, true),
        toggleDisable = preferences.getBoolean(KEY_USB_TOGGLE_DISABLE, true),
        alsoHideDevOptions = preferences.getBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, false),
        alsoDisableWirelessDebugging = preferences.getBoolean(KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING, false),
        enableAutoRevert = preferences.getBoolean(KEY_USB_ENABLE_AUTO_REVERT, false),
        autoRevertDelaySeconds = preferences.getInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, 5),
        requireUnlock = preferences.getBoolean(KEY_USB_REQUIRE_UNLOCK, false)
    )
    override fun setToggleEnable(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_TOGGLE_ENABLE, value) }
        refresh()
    }
    override fun setToggleDisable(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_TOGGLE_DISABLE, value) }
        refresh()
    }
    override fun setAlsoHideDevOptions(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, value) }
        refresh()
    }
    override fun setAlsoDisableWirelessDebugging(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING, value) }
        refresh()
    }
    override fun setEnableAutoRevert(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_ENABLE_AUTO_REVERT, value) }
        refresh()
    }
    override fun setAutoRevertDelaySeconds(value: Int) {
        preferences.edit { putInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, value) }
        refresh()
    }
    override fun setRequireUnlock(value: Boolean) {
        preferences.edit { putBoolean(KEY_USB_REQUIRE_UNLOCK, value) }
        refresh()
    }
    /** Joins the already validated cross-feature backup transaction. */
    fun writeTo(editor: SharedPreferences.Editor, settings: UsbSettings) {
        editor.putBoolean(KEY_USB_TOGGLE_ENABLE, settings.toggleEnable)
        editor.putBoolean(KEY_USB_TOGGLE_DISABLE, settings.toggleDisable)
        editor.putBoolean(KEY_USB_ALSO_HIDE_DEV_OPTIONS, settings.alsoHideDevOptions)
        editor.putBoolean(KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING, settings.alsoDisableWirelessDebugging)
        editor.putBoolean(KEY_USB_ENABLE_AUTO_REVERT, settings.enableAutoRevert)
        editor.putInt(KEY_USB_AUTO_REVERT_DELAY_SECONDS, settings.autoRevertDelaySeconds)
        editor.putBoolean(KEY_USB_REQUIRE_UNLOCK, settings.requireUnlock)
    }

    companion object {
        const val KEY_USB_TOGGLE_ENABLE = "usb_toggle_enable"
        const val KEY_USB_TOGGLE_DISABLE = "usb_toggle_disable"
        const val KEY_USB_ALSO_HIDE_DEV_OPTIONS = "usb_also_hide_dev_options"
        const val KEY_USB_ALSO_DISABLE_WIRELESS_DEBUGGING = "usb_also_disable_wireless_debugging"
        const val KEY_USB_ENABLE_AUTO_REVERT = "usb_enable_auto_revert"
        const val KEY_USB_AUTO_REVERT_DELAY_SECONDS = "usb_auto_revert_delay_seconds"
        const val KEY_USB_REQUIRE_UNLOCK = "usb_require_unlock"
    }
}
