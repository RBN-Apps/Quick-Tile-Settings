package com.rbn.qtsettings.feature.usb.domain

import kotlinx.coroutines.flow.StateFlow

interface UsbSettingsSource {
    val state: StateFlow<UsbSettings>
    fun read(): UsbSettings
    fun setToggleEnable(value: Boolean)
    fun setToggleDisable(value: Boolean)
    fun setAlsoHideDevOptions(value: Boolean)
    fun setAlsoDisableWirelessDebugging(value: Boolean)
    fun setEnableAutoRevert(value: Boolean)
    fun setAutoRevertDelaySeconds(value: Int)
    fun setRequireUnlock(value: Boolean)
}
