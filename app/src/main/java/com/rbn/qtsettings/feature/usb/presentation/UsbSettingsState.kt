package com.rbn.qtsettings.feature.usb.presentation

import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState

data class UsbSettingsState(
    val settings: UsbSettings = UsbSettings(),
    val system: UsbSystemState = UsbSystemState(false, false, false),
    val hasPermission: Boolean = false
)

sealed interface UsbSettingsAction {
    data class ToggleEnable(val value: Boolean) : UsbSettingsAction
    data class ToggleDisable(val value: Boolean) : UsbSettingsAction
    data class AlsoHideDevOptions(val value: Boolean) : UsbSettingsAction
    data class AlsoDisableWirelessDebugging(val value: Boolean) : UsbSettingsAction
    data class EnableAutoRevert(val value: Boolean) : UsbSettingsAction
    data class AutoRevertDelaySeconds(val value: Int) : UsbSettingsAction
    data class RequireUnlock(val value: Boolean) : UsbSettingsAction
    data class SetActive(val enabled: Boolean) : UsbSettingsAction
}
