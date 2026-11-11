package com.rbn.qtsettings.feature.usb.domain

data class UsbSystemState(val usb: Boolean, val developerOptions: Boolean, val wireless: Boolean) {
    val active: Boolean get() = usb && developerOptions
}
enum class UsbSetting { Usb, DeveloperOptions, Wireless }
enum class UsbActionResult { Success, PermissionMissing, DeveloperOptionsDisabled, NoCycleStates, Failed, PartialFailure }

interface UsbSystemGateway {
    fun hasPermission(): Boolean
    fun read(): UsbSystemState
    fun write(setting: UsbSetting, enabled: Boolean): Boolean
    fun generation(): Long
    fun invalidateRevert()
}
