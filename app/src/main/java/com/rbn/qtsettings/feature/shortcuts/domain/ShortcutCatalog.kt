package com.rbn.qtsettings.feature.shortcuts.domain

import com.rbn.qtsettings.data.DnsHostnameEntry

/** Stable IDs and ordering shared by persistence, launcher publishing and execution. */
object ShortcutCatalog {
    const val MAX_FAVORITE_SHORTCUTS = 4

    const val SHORTCUT_ID_DNS_OFF = "dyn_dns_off"
    const val SHORTCUT_ID_DNS_AUTO = "dyn_dns_auto"
    const val SHORTCUT_ID_DNS_ADGUARD = "dyn_dns_adguard"
    const val SHORTCUT_ID_DNS_CLOUDFLARE = "dyn_dns_cloudflare"
    const val SHORTCUT_ID_DNS_QUAD9 = "dyn_dns_quad9"
    const val SHORTCUT_ID_USB_ON = "dyn_usb_on"
    const val SHORTCUT_ID_USB_OFF = "dyn_usb_off"

    private val legacyShortcutIdMap = mapOf(
        "dns_off" to SHORTCUT_ID_DNS_OFF,
        "dns_auto" to SHORTCUT_ID_DNS_AUTO,
        "dns_adguard" to SHORTCUT_ID_DNS_ADGUARD,
        "dns_cloudflare" to SHORTCUT_ID_DNS_CLOUDFLARE,
        "dns_quad9" to SHORTCUT_ID_DNS_QUAD9,
        "usb_on" to SHORTCUT_ID_USB_ON,
        "usb_off" to SHORTCUT_ID_USB_OFF
    )

    private val builtInShortcutIds = setOf(SHORTCUT_ID_DNS_OFF, SHORTCUT_ID_DNS_AUTO,
        SHORTCUT_ID_DNS_ADGUARD, SHORTCUT_ID_DNS_CLOUDFLARE, SHORTCUT_ID_DNS_QUAD9,
        SHORTCUT_ID_USB_ON, SHORTCUT_ID_USB_OFF)
    private val predefinedDnsShortcutIdMap = mapOf(
        "adguard_default" to SHORTCUT_ID_DNS_ADGUARD,
        "cloudflare_default" to SHORTCUT_ID_DNS_CLOUDFLARE,
        "quad9_default" to SHORTCUT_ID_DNS_QUAD9
    )

    fun getShortcutIdForDnsEntry(entry: DnsHostnameEntry): String {
        if (entry.isPredefined) {
            return predefinedDnsShortcutIdMap[entry.id] ?: createCustomDnsShortcutId(entry.id)
        }
        return createCustomDnsShortcutId(entry.id)
    }

    fun getAvailableShortcutIds(hostnames: List<DnsHostnameEntry>): Set<String> {
        val availableDnsIds = hostnames.map { getShortcutIdForDnsEntry(it) }
        return builtInShortcutIds + availableDnsIds
    }

    fun migrateLegacyShortcutIds(shortcutIds: Set<String>): Set<String> {
        return shortcutIds.map { shortcutId -> legacyShortcutIdMap[shortcutId] ?: shortcutId }
            .toSet()
    }

    fun getOrderedShortcutIds(
        hostnames: List<DnsHostnameEntry>,
        favoriteShortcutIds: Set<String> = emptySet()
    ): List<String> {
        val predefinedDnsIds = listOf(
            SHORTCUT_ID_DNS_ADGUARD,
            SHORTCUT_ID_DNS_CLOUDFLARE,
            SHORTCUT_ID_DNS_QUAD9
        )
        val orderedDnsIds = hostnames.map { getShortcutIdForDnsEntry(it) }
        val missingPredefinedDnsIds = predefinedDnsIds.filterNot(orderedDnsIds::contains)
        val canonicalOrder = (
            listOf(
                SHORTCUT_ID_DNS_OFF,
                SHORTCUT_ID_DNS_AUTO
            ) + orderedDnsIds + missingPredefinedDnsIds + listOf(
                SHORTCUT_ID_USB_ON,
                SHORTCUT_ID_USB_OFF
            )
        ).distinct()
        if (favoriteShortcutIds.isEmpty()) {
            return canonicalOrder
        }

        val favoritesFirst = canonicalOrder.filter { favoriteShortcutIds.contains(it) }
        val remaining = canonicalOrder.filterNot { favoriteShortcutIds.contains(it) }
        return favoritesFirst + remaining
    }

    private fun createCustomDnsShortcutId(entryId: String) = "$CUSTOM_DNS_SHORTCUT_PREFIX$entryId"
    const val CUSTOM_DNS_SHORTCUT_PREFIX = "dns_custom_"
}
