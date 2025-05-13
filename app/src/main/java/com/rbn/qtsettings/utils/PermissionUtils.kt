package com.rbn.qtsettings.utils

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

object PermissionUtils {
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return Settings.Global.getInt(context.contentResolver, Constants.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    }
}