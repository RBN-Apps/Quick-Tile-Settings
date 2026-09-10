package com.rbn.qtsettings.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway
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
        return UsbActions(
            AndroidUsbSystemGateway(context)
        ).set(enabled, toggleDeveloperOptions, toggleWirelessDebugging).toQuickActionResult()
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
        AndroidUsbSystemGateway(context).read().active
    }.getOrDefault(false)
}

private fun UsbActionResult.toQuickActionResult(): SystemQuickActionResult = when (this) {
    UsbActionResult.Success -> SystemQuickActionResult.SUCCESS
    UsbActionResult.PermissionMissing -> SystemQuickActionResult.PERMISSION_MISSING
    UsbActionResult.DeveloperOptionsDisabled -> SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED
    else -> SystemQuickActionResult.FAILED
}
