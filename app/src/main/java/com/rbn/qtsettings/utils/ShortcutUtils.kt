package com.rbn.qtsettings.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ShortcutActionActivity
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_ADGUARD
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_AUTO
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CLOUDFLARE
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CUSTOM
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_QUAD9
import com.rbn.qtsettings.utils.Constants.ACTION_USB_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_USB_ON
import com.rbn.qtsettings.utils.Constants.EXTRA_DNS_ENTRY_ID
import java.util.Locale

object ShortcutUtils {
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

    private val builtInShortcutDefinitions = listOf(
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_DNS_OFF,
            action = ACTION_DNS_OFF,
            iconRes = R.drawable.ic_shortcut_dns_off,
            shortLabelRes = R.string.shortcut_dns_off_short,
            longLabelRes = R.string.shortcut_dns_off_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_DNS_AUTO,
            action = ACTION_DNS_AUTO,
            iconRes = R.drawable.ic_shortcut_dns_auto,
            shortLabelRes = R.string.shortcut_dns_auto_short,
            longLabelRes = R.string.shortcut_dns_auto_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_DNS_ADGUARD,
            action = ACTION_DNS_ADGUARD,
            iconRes = R.drawable.ic_shortcut_adguard,
            shortLabelRes = R.string.shortcut_dns_adguard_short,
            longLabelRes = R.string.shortcut_dns_adguard_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_DNS_CLOUDFLARE,
            action = ACTION_DNS_CLOUDFLARE,
            iconRes = R.drawable.ic_shortcut_cloudflare,
            shortLabelRes = R.string.shortcut_dns_cloudflare_short,
            longLabelRes = R.string.shortcut_dns_cloudflare_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_DNS_QUAD9,
            action = ACTION_DNS_QUAD9,
            iconRes = R.drawable.ic_shortcut_quad9,
            shortLabelRes = R.string.shortcut_dns_quad9_short,
            longLabelRes = R.string.shortcut_dns_quad9_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_USB_ON,
            action = ACTION_USB_ON,
            iconRes = R.drawable.ic_shortcut_usb_on,
            shortLabelRes = R.string.shortcut_usb_on_short,
            longLabelRes = R.string.shortcut_usb_on_long
        ),
        BuiltInShortcutDefinition(
            id = SHORTCUT_ID_USB_OFF,
            action = ACTION_USB_OFF,
            iconRes = R.drawable.ic_shortcut_usb_off,
            shortLabelRes = R.string.shortcut_usb_off_short,
            longLabelRes = R.string.shortcut_usb_off_long
        )
    )

    private val builtInShortcutIds = builtInShortcutDefinitions.map { it.id }.toSet()
    private val shortcutIdByAction = builtInShortcutDefinitions.associate { it.action to it.id }

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

    fun getShortcutIdForAction(action: String?): String? = shortcutIdByAction[action]

    fun getCustomEntryIdFromShortcutId(shortcutId: String): String? {
        return shortcutId.removePrefix(CUSTOM_DNS_SHORTCUT_PREFIX)
            .takeIf { shortcutId.startsWith(CUSTOM_DNS_SHORTCUT_PREFIX) && it.isNotBlank() }
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
        val baseOrderedIds = mutableListOf(
            SHORTCUT_ID_DNS_OFF,
            SHORTCUT_ID_DNS_AUTO,
            SHORTCUT_ID_DNS_ADGUARD,
            SHORTCUT_ID_DNS_CLOUDFLARE,
            SHORTCUT_ID_DNS_QUAD9,
            SHORTCUT_ID_USB_ON,
            SHORTCUT_ID_USB_OFF
        )

        val customIds = hostnames
            .filter { !it.isPredefined }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { getShortcutIdForDnsEntry(it) }
        baseOrderedIds.addAll(customIds)
        val canonicalOrder = baseOrderedIds.distinct()
        if (favoriteShortcutIds.isEmpty()) {
            return canonicalOrder
        }

        val favoritesFirst = canonicalOrder.filter { favoriteShortcutIds.contains(it) }
        val remaining = canonicalOrder.filterNot { favoriteShortcutIds.contains(it) }
        return favoritesFirst + remaining
    }

    fun getMaxShortcutCount(context: Context): Int {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            ?: return builtInShortcutIds.size
        return shortcutManager.maxShortcutCountPerActivity.coerceAtLeast(1)
    }

    fun updateExposedShortcuts(
        context: Context,
        dnsHostnames: List<DnsHostnameEntry>,
        enabledShortcutIds: Set<String>,
        favoriteShortcutIds: Set<String>
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val availableShortcutIds = getAvailableShortcutIds(dnsHostnames)
        val effectiveEnabledIds = enabledShortcutIds.intersect(availableShortcutIds)
        val effectiveFavoriteIds = favoriteShortcutIds.intersect(availableShortcutIds)
        val builtInShortcutsById = builtInShortcutDefinitions
            .filter { effectiveEnabledIds.contains(it.id) }
            .associateBy { it.id }

        val customShortcutsById = dnsHostnames
            .filter { !it.isPredefined }
            .filter { effectiveEnabledIds.contains(getShortcutIdForDnsEntry(it)) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .associateBy { getShortcutIdForDnsEntry(it) }

        val orderedEnabledShortcutIds = getOrderedShortcutIds(
            hostnames = dnsHostnames,
            favoriteShortcutIds = effectiveFavoriteIds
        )
            .filter { shortcutId ->
                builtInShortcutsById.containsKey(shortcutId) ||
                        customShortcutsById.containsKey(shortcutId)
            }
            .take(shortcutManager.maxShortcutCountPerActivity)

        val orderedShortcutInfos = orderedEnabledShortcutIds
            .mapIndexedNotNull { rank, shortcutId ->
                val builtInDefinition = builtInShortcutsById[shortcutId]
                if (builtInDefinition != null) {
                    ShortcutInfo.Builder(context, builtInDefinition.id)
                        .setShortLabel(context.getString(builtInDefinition.shortLabelRes))
                        .setLongLabel(context.getString(builtInDefinition.longLabelRes))
                        .setIcon(Icon.createWithResource(context, builtInDefinition.iconRes))
                        .setIntent(Intent(context, ShortcutActionActivity::class.java).apply {
                            action = builtInDefinition.action
                        })
                        .setRank(rank)
                        .build()
                } else {
                    val entry = customShortcutsById[shortcutId] ?: return@mapIndexedNotNull null
                    ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(entry.name)
                        .setLongLabel(
                            context.getString(
                                R.string.shortcut_dns_custom_long,
                                entry.name
                            )
                        )
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_dns_on))
                        .setIntent(Intent(context, ShortcutActionActivity::class.java).apply {
                            action = ACTION_DNS_CUSTOM
                            putExtra(EXTRA_DNS_ENTRY_ID, entry.id)
                        })
                        .setRank(rank)
                        .build()
                }
            }

        try {
            shortcutManager.dynamicShortcuts = orderedShortcutInfos
        } catch (e: IllegalArgumentException) {
            Log.e(
                "ShortcutUtils",
                "Unable to publish dynamic shortcuts. Falling back to custom DNS shortcuts only.",
                e
            )
            val customOnly =
                orderedShortcutInfos.filter { it.id.startsWith(CUSTOM_DNS_SHORTCUT_PREFIX) }
            try {
                shortcutManager.dynamicShortcuts = customOnly
            } catch (fallbackError: Exception) {
                Log.e(
                    "ShortcutUtils",
                    "Fallback dynamic shortcut publish also failed.",
                    fallbackError
                )
            }
        }
    }

    private fun createCustomDnsShortcutId(entryId: String): String =
        "$CUSTOM_DNS_SHORTCUT_PREFIX$entryId"

    private data class BuiltInShortcutDefinition(
        val id: String,
        val action: String,
        val iconRes: Int,
        val shortLabelRes: Int,
        val longLabelRes: Int
    )

    private const val CUSTOM_DNS_SHORTCUT_PREFIX = "dns_custom_"
}
