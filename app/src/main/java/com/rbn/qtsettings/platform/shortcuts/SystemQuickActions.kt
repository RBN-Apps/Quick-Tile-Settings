package com.rbn.qtsettings.platform.shortcuts

import android.content.Context
import com.rbn.qtsettings.feature.dns.data.toPersisted
import com.rbn.qtsettings.feature.dns.domain.DnsActionResult
import com.rbn.qtsettings.feature.dns.domain.DnsActions
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.platform.dns.AndroidDnsSystemGateway
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF

enum class SystemQuickActionResult {
    SUCCESS,
    PERMISSION_MISSING,
    DEVELOPER_OPTIONS_DISABLED,
    INVALID_DNS_HOSTNAME,
    FAILED
}

/** Executes privileged actions initiated directly from the app UI. */
object SystemQuickActions {
    private fun dnsActions(context: Context) = DnsActions(AndroidDnsSystemGateway(context))

    fun setActiveDns(context: Context, hostname: String): SystemQuickActionResult =
        dnsActions(context).set(DnsTarget.Hostname(hostname)).toQuickActionResult()

    fun setDnsMode(context: Context, mode: String): SystemQuickActionResult {
        val actions = dnsActions(context)
        if (!actions.hasPermission()) return SystemQuickActionResult.PERMISSION_MISSING
        val target = when (mode) {
            DNS_MODE_OFF -> DnsTarget.Off
            DNS_MODE_AUTO -> DnsTarget.Automatic
            else -> return SystemQuickActionResult.FAILED
        }
        return actions.set(target).toQuickActionResult()
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

    fun getActiveDnsHostname(context: Context): String? =
        (dnsActions(context).read() as? DnsTarget.Hostname)?.value

    fun getActiveDnsMode(context: Context): String? = dnsActions(context).read()?.toPersisted()?.mode

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

private fun DnsActionResult.toQuickActionResult(): SystemQuickActionResult = when (this) {
    DnsActionResult.Success -> SystemQuickActionResult.SUCCESS
    DnsActionResult.PermissionMissing -> SystemQuickActionResult.PERMISSION_MISSING
    DnsActionResult.InvalidHostname -> SystemQuickActionResult.INVALID_DNS_HOSTNAME
    else -> SystemQuickActionResult.FAILED
}
