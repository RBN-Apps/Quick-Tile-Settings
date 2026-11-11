package com.rbn.qtsettings.feature.usb.domain

data class UsbSettings(
    val toggleEnable: Boolean = true,
    val toggleDisable: Boolean = true,
    val alsoHideDevOptions: Boolean = false,
    val alsoDisableWirelessDebugging: Boolean = false,
    val enableAutoRevert: Boolean = false,
    val autoRevertDelaySeconds: Int = 5,
    val requireUnlock: Boolean = false
)
