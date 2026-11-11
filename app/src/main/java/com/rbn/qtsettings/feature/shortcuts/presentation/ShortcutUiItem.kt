package com.rbn.qtsettings.feature.shortcuts.presentation

import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

internal fun buildPrivateDnsShortcutItems(dnsHostnames: List<DnsHostnameEntry>): List<ShortcutUiItem> {
    val items = mutableListOf(
        ShortcutUiItem(
            id = ShortcutCatalog.SHORTCUT_ID_DNS_OFF,
            title = ShortcutText.Resource(R.string.shortcut_dns_off_short),
            subtitle = ShortcutText.Resource(R.string.shortcut_dns_off_long),
            iconRes = R.drawable.ic_dns_off
        ),
        ShortcutUiItem(
            id = ShortcutCatalog.SHORTCUT_ID_DNS_AUTO,
            title = ShortcutText.Resource(R.string.shortcut_dns_auto_short),
            subtitle = ShortcutText.Resource(R.string.shortcut_dns_auto_long),
            iconRes = R.drawable.ic_dns_auto
        )
    )

    dnsHostnames.forEach { entry ->
        val shortcutId = ShortcutCatalog.getShortcutIdForDnsEntry(entry)
        if (entry.isPredefined) {
            val predefinedItem = when (shortcutId) {
                ShortcutCatalog.SHORTCUT_ID_DNS_ADGUARD -> ShortcutUiItem(
                    id = shortcutId,
                    title = ShortcutText.Resource(R.string.shortcut_dns_adguard_short),
                    subtitle = ShortcutText.Value(entry.hostname),
                    iconRes = R.drawable.ic_dns_on_adguard
                )

                ShortcutCatalog.SHORTCUT_ID_DNS_CLOUDFLARE -> ShortcutUiItem(
                    id = shortcutId,
                    title = ShortcutText.Resource(R.string.shortcut_dns_cloudflare_short),
                    subtitle = ShortcutText.Value(entry.hostname),
                    iconRes = R.drawable.ic_dns_on_cloudflare
                )

                ShortcutCatalog.SHORTCUT_ID_DNS_QUAD9 -> ShortcutUiItem(
                    id = shortcutId,
                    title = ShortcutText.Resource(R.string.shortcut_dns_quad9_short),
                    subtitle = ShortcutText.Value(entry.hostname),
                    iconRes = R.drawable.ic_dns_on_quad9_security
                )

                else -> null
            }
            predefinedItem?.let { items.add(it) }
        } else {
            items.add(
                ShortcutUiItem(
                    id = shortcutId,
                    title = ShortcutText.Value(entry.name),
                    subtitle = ShortcutText.Value(entry.hostname),
                    iconRes = R.drawable.ic_dns_on
                )
            )
        }
    }

    return items
}

internal data class ShortcutUiItem(
    val id: String,
    val title: ShortcutText,
    val subtitle: ShortcutText,
    val iconRes: Int
)

internal fun buildUsbShortcutItems() = listOf(
        ShortcutUiItem(
            id = ShortcutCatalog.SHORTCUT_ID_USB_ON,
            title = ShortcutText.Resource(R.string.shortcut_usb_on_short),
            subtitle = ShortcutText.Resource(R.string.shortcut_usb_on_long),
            iconRes = R.drawable.ic_usb_on
        ),
        ShortcutUiItem(
            id = ShortcutCatalog.SHORTCUT_ID_USB_OFF,
            title = ShortcutText.Resource(R.string.shortcut_usb_off_short),
            subtitle = ShortcutText.Resource(R.string.shortcut_usb_off_long),
            iconRes = R.drawable.ic_usb_off
        )
    )
