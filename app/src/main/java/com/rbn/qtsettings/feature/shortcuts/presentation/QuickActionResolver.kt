package com.rbn.qtsettings.feature.shortcuts.presentation

import android.content.ComponentName
import android.content.Context
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.dns.domain.DnsProvider
import com.rbn.qtsettings.feature.dns.domain.DnsSettings
import com.rbn.qtsettings.feature.dns.presentation.dnsIconForHostname
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService

internal enum class TileType {
    DNS,
    USB,
    ALL
}

internal data class QuickAction(
    val label: String,
    val description: String,
    val iconRes: Int,
    val kind: QuickActionKind
)

internal sealed interface QuickActionKind {
    data object DnsOff : QuickActionKind
    data object DnsAuto : QuickActionKind
    data class DnsHostname(val entry: DnsProvider) : QuickActionKind
    data class UsbDebugging(val enable: Boolean) : QuickActionKind
}

internal object QuickActionResolver {

    fun resolveTileType(
        componentName: ComponentName?,
        componentNameText: String?
    ): TileType {
        return when {
            componentName?.className == UsbDebuggingTileService::class.java.name ||
                    componentNameText?.contains(UsbDebuggingTileService::class.java.name) == true ->
                TileType.USB

            componentName?.className == PrivateDnsTileService::class.java.name ||
                    componentNameText?.contains(PrivateDnsTileService::class.java.name) == true ->
                TileType.DNS

            else -> TileType.ALL
        }
    }

    fun buildQuickActions(
        context: Context,
        dns: DnsSettings,
        usb: UsbSettings,
        tileType: TileType
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (tileType == TileType.DNS || tileType == TileType.ALL) {
            actions += buildDnsActions(context, dns)
        }
        if (tileType == TileType.USB || tileType == TileType.ALL) {
            actions += buildUsbActions(context, usb)
        }
        return actions
    }

    private fun buildDnsActions(
        context: Context,
        dns: DnsSettings
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (dns.toggleOff) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_dns_off_short),
                description = context.getString(R.string.shortcut_dns_off_long),
                iconRes = R.drawable.ic_dns_off,
                kind = QuickActionKind.DnsOff
            )
        }
        if (dns.toggleAuto) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_dns_auto_short),
                description = context.getString(R.string.shortcut_dns_auto_long),
                iconRes = R.drawable.ic_dns_auto,
                kind = QuickActionKind.DnsAuto
            )
        }
        dns.providers.filter { it.selectedForCycle }.forEach { entry ->
            actions += QuickAction(
                label = entry.name,
                description = entry.target.value,
                iconRes = getDnsIcon(entry),
                kind = QuickActionKind.DnsHostname(entry)
            )
        }
        return actions
    }

    private fun buildUsbActions(
        context: Context,
        usb: UsbSettings
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (usb.toggleEnable) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_usb_on_short),
                description = context.getString(R.string.shortcut_usb_on_long),
                iconRes = R.drawable.ic_usb_on,
                kind = QuickActionKind.UsbDebugging(enable = true)
            )
        }
        if (usb.toggleDisable) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_usb_off_short),
                description = context.getString(R.string.shortcut_usb_off_long),
                iconRes = R.drawable.ic_usb_off,
                kind = QuickActionKind.UsbDebugging(enable = false)
            )
        }
        return actions
    }

    private fun getDnsIcon(entry: DnsProvider): Int = dnsIconForHostname(entry.target.value)
}
