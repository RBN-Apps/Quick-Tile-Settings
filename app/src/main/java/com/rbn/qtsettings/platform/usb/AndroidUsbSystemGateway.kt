package com.rbn.qtsettings.platform.usb

import android.content.Context
import android.provider.Settings
import com.rbn.qtsettings.feature.usb.domain.*
import com.rbn.qtsettings.platform.revert.AutoRevertCoordinator
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.platform.permissions.PermissionUtils

class AndroidUsbSystemGateway(context: Context) : UsbSystemGateway {
    private val context = context.applicationContext
    override fun hasPermission() = PermissionUtils.hasWriteSecureSettingsPermission(context)
    override fun read() = UsbSystemState(
        DebuggingSettingsReader.isUsbDebuggingEnabled(context.contentResolver),
        DebuggingSettingsReader.isDeveloperOptionsEnabled(context.contentResolver),
        Settings.Global.getInt(context.contentResolver, Constants.ADB_WIFI_ENABLED, 0) == 1
    )
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
