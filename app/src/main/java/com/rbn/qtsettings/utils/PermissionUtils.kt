package com.rbn.qtsettings.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import rikka.shizuku.Shizuku
import java.io.File

object PermissionUtils {
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 12345

    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Constants.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }

    fun isShizukuAvailableAndReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun checkShizukuPermission(context: Context): Int {
        if (!isShizukuAvailableAndReady()) return PackageManager.PERMISSION_DENIED

        return try {
            if (Shizuku.isPreV11()) {
                if (context.checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED) {
                    PackageManager.PERMISSION_GRANTED
                } else {
                    PackageManager.PERMISSION_DENIED
                }
            } else {
                Shizuku.checkSelfPermission()
            }
        } catch (e: IllegalStateException) {
            PackageManager.PERMISSION_DENIED
        }
    }

    fun requestShizukuPermission(activity: Activity) {
        if (!isShizukuAvailableAndReady()) return

        try {
            if (Shizuku.isPreV11()) {
                activity.requestPermissions(
                    arrayOf("moe.shizuku.manager.permission.API_V23"),
                    SHIZUKU_PERMISSION_REQUEST_CODE
                )
            } else if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
        } catch (e: IllegalStateException) {
            /* Shizuku service not running */
        }
    }

    fun isDeviceRooted(): Boolean {
        val suBinaries = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in suBinaries) {
            if (File(path).exists()) {
                return true
            }
        }
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))

            process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }
}