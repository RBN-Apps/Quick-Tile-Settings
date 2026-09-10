package com.rbn.qtsettings.platform.usb

import android.content.Context
import android.provider.Settings
import com.rbn.qtsettings.feature.usb.domain.*
import com.rbn.qtsettings.utils.AutoRevertCoordinator
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.PermissionUtils

class AndroidUsbSystemGateway(context: Context) : UsbSystemGateway {
    private val context = context.applicationContext
    override fun hasPermission() = PermissionUtils.hasWriteSecureSettingsPermission(context)
    override fun read() = UsbSystemState(
        get(Constants.ADB_ENABLED), get(Constants.DEVELOPMENT_SETTINGS_ENABLED), get(Constants.ADB_WIFI_ENABLED)
    )
    private fun get(key: String) = Settings.Global.getInt(context.contentResolver, key, 0) == 1
    override fun write(setting: UsbSetting, enabled: Boolean): Boolean = Settings.Global.putInt(
        context.contentResolver,
        when (setting) {
            UsbSetting.Usb -> Constants.ADB_ENABLED
            UsbSetting.DeveloperOptions -> Constants.DEVELOPMENT_SETTINGS_ENABLED
            UsbSetting.Wireless -> Constants.ADB_WIFI_ENABLED
        },
        if (enabled) 1 else 0
    )
    override fun generation() = AutoRevertCoordinator.usbGeneration(context)
    override fun invalidateRevert() = AutoRevertCoordinator.invalidateUsbAutoRevert(context)
}
