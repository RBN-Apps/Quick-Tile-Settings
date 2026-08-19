package com.rbn.qtsettings.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.rbn.qtsettings.utils.Constants.ADB_ENABLED
import com.rbn.qtsettings.utils.Constants.ADB_WIFI_ENABLED
import com.rbn.qtsettings.utils.Constants.DEVELOPMENT_SETTINGS_ENABLED
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER

enum class SystemQuickActionResult {
    SUCCESS,
    PERMISSION_MISSING,
    DEVELOPER_OPTIONS_DISABLED,
    INVALID_DNS_HOSTNAME,
    FAILED
}

/** Executes privileged actions initiated directly from the app UI. */
object SystemQuickActions {
    private const val TAG = "SystemQuickActions"

    fun setActiveDns(context: Context, hostname: String): SystemQuickActionResult {
        if (!PermissionUtils.hasWriteSecureSettingsPermission(context)) {
            return SystemQuickActionResult.PERMISSION_MISSING
        }

        val normalizedHostname = hostname.trim()
        if (!DnsHostnameValidator.isValid(normalizedHostname)) {
            return SystemQuickActionResult.INVALID_DNS_HOSTNAME
        }

        val resolver = context.contentResolver
        var previousMode: String? = null
        var previousSpecifier: String? = null
        var previousStateCaptured = false
        return try {
            previousMode = Settings.Global.getString(resolver, PRIVATE_DNS_MODE)
            previousSpecifier = Settings.Global.getString(resolver, PRIVATE_DNS_SPECIFIER)
            previousStateCaptured = true

            val specifierSaved = Settings.Global.putString(
                resolver,
                PRIVATE_DNS_SPECIFIER,
                normalizedHostname
            )
            if (!specifierSaved) {
                SystemQuickActionResult.FAILED
            } else if (Settings.Global.putString(resolver, PRIVATE_DNS_MODE, DNS_MODE_ON)) {
                AutoRevertCoordinator.invalidateDnsAutoRevert(context)
                SystemQuickActionResult.SUCCESS
            } else {
                restoreDnsState(resolver, previousMode, previousSpecifier)
                SystemQuickActionResult.FAILED
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to activate Private DNS", error)
            if (previousStateCaptured) {
                restoreDnsState(resolver, previousMode, previousSpecifier)
            }
            SystemQuickActionResult.FAILED
        }
    }

    /** Activates the modeless Private DNS states ("off" or "opportunistic"). */
    fun setDnsMode(context: Context, mode: String): SystemQuickActionResult {
        if (!PermissionUtils.hasWriteSecureSettingsPermission(context)) {
            return SystemQuickActionResult.PERMISSION_MISSING
        }
        if (mode != DNS_MODE_OFF && mode != DNS_MODE_AUTO) {
            return SystemQuickActionResult.FAILED
        }

        val resolver = context.contentResolver
        return try {
            if (Settings.Global.putString(resolver, PRIVATE_DNS_MODE, mode)) {
                AutoRevertCoordinator.invalidateDnsAutoRevert(context)
                SystemQuickActionResult.SUCCESS
            } else {
                SystemQuickActionResult.FAILED
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to change Private DNS mode", error)
            SystemQuickActionResult.FAILED
        }
    }

    fun setUsbDebuggingEnabled(
        context: Context,
        enabled: Boolean,
        toggleDeveloperOptions: Boolean,
        toggleWirelessDebugging: Boolean
    ): SystemQuickActionResult {
        if (!PermissionUtils.hasWriteSecureSettingsPermission(context)) {
            return SystemQuickActionResult.PERMISSION_MISSING
        }
        if (!toggleDeveloperOptions && !PermissionUtils.isDeveloperOptionsEnabled(context)) {
            return SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED
        }

        val resolver = context.contentResolver
        var previousDeveloperOptions = 0
        var previousUsbDebugging = 0
        var previousWirelessDebugging = 0
        var previousStateCaptured = false
        return try {
            previousDeveloperOptions = Settings.Global.getInt(
                resolver,
                DEVELOPMENT_SETTINGS_ENABLED,
                0
            )
            previousUsbDebugging = Settings.Global.getInt(resolver, ADB_ENABLED, 0)
            previousWirelessDebugging = Settings.Global.getInt(resolver, ADB_WIFI_ENABLED, 0)
            previousStateCaptured = true

            val developerOptionsSaved = when {
                !toggleDeveloperOptions -> true
                enabled -> Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
                else -> true
            }
            val usbDebuggingSaved = developerOptionsSaved && Settings.Global.putInt(
                resolver,
                ADB_ENABLED,
                if (enabled) 1 else 0
            )
            val wirelessDebuggingSaved = usbDebuggingSaved && (
                !toggleWirelessDebugging || Settings.Global.putInt(
                    resolver,
                    ADB_WIFI_ENABLED,
                    if (enabled) 1 else 0
                )
            )
            val developerOptionsDisabled = wirelessDebuggingSaved && (
                !toggleDeveloperOptions || enabled || Settings.Global.putInt(
                    resolver,
                    DEVELOPMENT_SETTINGS_ENABLED,
                    0
                )
            )

            if (developerOptionsDisabled) {
                AutoRevertCoordinator.invalidateUsbAutoRevert(context)
                SystemQuickActionResult.SUCCESS
            } else {
                restoreUsbState(
                    context = context,
                    developerOptions = previousDeveloperOptions,
                    usbDebugging = previousUsbDebugging,
                    wirelessDebugging = previousWirelessDebugging
                )
                SystemQuickActionResult.FAILED
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to change USB debugging", error)
            if (previousStateCaptured) {
                restoreUsbState(
                    context = context,
                    developerOptions = previousDeveloperOptions,
                    usbDebugging = previousUsbDebugging,
                    wirelessDebugging = previousWirelessDebugging
                )
            }
            SystemQuickActionResult.FAILED
        }
    }

    private fun restoreDnsState(
        resolver: android.content.ContentResolver,
        mode: String?,
        specifier: String?
    ) {
        runCatching {
            Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, specifier)
            Settings.Global.putString(resolver, PRIVATE_DNS_MODE, mode)
        }.onFailure { error -> Log.e(TAG, "Unable to restore Private DNS state", error) }
    }

    private fun restoreUsbState(
        context: Context,
        developerOptions: Int,
        usbDebugging: Int,
        wirelessDebugging: Int
    ) {
        runCatching {
            val resolver = context.contentResolver
            if (developerOptions == 1) {
                Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
            }
            Settings.Global.putInt(resolver, ADB_ENABLED, usbDebugging)
            Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, wirelessDebugging)
            Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, developerOptions)
        }.onFailure { error -> Log.e(TAG, "Unable to restore USB debugging state", error) }
    }

    fun getActiveDnsHostname(context: Context): String? = runCatching {
        val mode = Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE)
        if (mode == DNS_MODE_ON) {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER)
                ?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }.getOrNull()

    fun getActiveDnsMode(context: Context): String? = runCatching {
        Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE)
    }.getOrNull()

    fun isUsbDebuggingEnabled(context: Context): Boolean = runCatching {
        PermissionUtils.isDeveloperOptionsEnabled(context) &&
                Settings.Global.getInt(context.contentResolver, ADB_ENABLED, 0) == 1
    }.getOrDefault(false)
}
