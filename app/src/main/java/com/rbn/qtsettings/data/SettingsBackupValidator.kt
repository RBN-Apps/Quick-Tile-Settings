package com.rbn.qtsettings.data

import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.ShortcutUtils

internal data class ValidatedSettingsBackup(
    val dns: DnsSettingsBackup,
    val usb: UsbSettingsBackup,
    val shortcuts: ShortcutSettingsBackup
)

internal class SettingsBackupValidator(
    private val shortcutMaxCount: Int
) {
    private val validDnsModes = setOf(DNS_MODE_OFF, DNS_MODE_AUTO, DNS_MODE_ON)
    private val validDetectionModes = setOf(TILE_ONLY_DETECTION, BACKGROUND_DETECTION)

    fun validate(backup: SettingsBackup): ValidatedSettingsBackup {
        val dns = backup.dns ?: throw IllegalArgumentException("Backup is missing DNS settings")
        val usb = backup.usb ?: throw IllegalArgumentException("Backup is missing USB settings")
        val shortcuts = backup.shortcuts
            ?: throw IllegalArgumentException("Backup is missing shortcut settings")

        val restoredHostnames = DnsHostnamePolicy.reconcile(
            storedHostnames = dns.hostnames,
            normalizeCustomEntries = true,
            maxCustomEntries = MAX_RESTORED_CUSTOM_HOSTNAMES
        )
        val restoredSortMode = DnsListSortMode.fromPersistedValue(dns.sortMode)
        val effectiveRestoredHostnames = DnsHostnamePolicy.sort(restoredHostnames, restoredSortMode)
        val availableHostnames = restoredHostnames.map { it.hostname }.toSet()
        val availableShortcutIds = ShortcutUtils.getAvailableShortcutIds(restoredHostnames)
        val favoriteShortcutIds = normalizeFavoriteShortcutIds(
            favoriteShortcutIds = shortcuts.favoriteShortcutIds,
            enabledShortcutIds = shortcuts.enabledShortcutIds,
            availableShortcutIds = availableShortcutIds,
            restoredHostnames = effectiveRestoredHostnames
        )
        val enabledShortcutIds = normalizeEnabledShortcutIds(
            shortcutIds = shortcuts.enabledShortcutIds,
            favoriteShortcutIds = favoriteShortcutIds,
            availableShortcutIds = availableShortcutIds,
            restoredHostnames = effectiveRestoredHostnames
        )
        val wifiState = normalizeDnsState(dns.dnsStateOnWifi, DNS_MODE_OFF)
        val mobileState = normalizeDnsState(dns.dnsStateOnMobile, DNS_MODE_AUTO)

        return ValidatedSettingsBackup(
            dns = dns.copy(
                hostnames = restoredHostnames,
                sortMode = restoredSortMode.persistedValue,
                autoRevertDelaySeconds = normalizeDelaySeconds(dns.autoRevertDelaySeconds),
                vpnDetectionMode = normalizeDetectionMode(dns.vpnDetectionMode),
                networkTypeDetectionMode = normalizeDetectionMode(dns.networkTypeDetectionMode),
                dnsStateOnWifi = wifiState,
                dnsHostnameOnWifi = DnsHostnamePolicy.normalizeStateHostname(
                    wifiState,
                    dns.dnsHostnameOnWifi,
                    availableHostnames,
                    DNS_MODE_ON
                ),
                dnsStateOnMobile = mobileState,
                dnsHostnameOnMobile = DnsHostnamePolicy.normalizeStateHostname(
                    mobileState,
                    dns.dnsHostnameOnMobile,
                    availableHostnames,
                    DNS_MODE_ON
                )
            ),
            usb = usb.copy(
                autoRevertDelaySeconds = normalizeDelaySeconds(usb.autoRevertDelaySeconds)
            ),
            shortcuts = ShortcutSettingsBackup(
                enabledShortcutIds = enabledShortcutIds,
                favoriteShortcutIds = favoriteShortcutIds.intersect(enabledShortcutIds),
                allowPinnedShortcutsWhenDisabled = shortcuts.allowPinnedShortcutsWhenDisabled
            )
        )
    }

    private fun normalizeEnabledShortcutIds(
        shortcutIds: Set<String>?,
        favoriteShortcutIds: Set<String>,
        availableShortcutIds: Set<String>,
        restoredHostnames: List<DnsHostnameEntry>
    ): Set<String> {
        val migratedIds = normalizeShortcutIdSet(shortcutIds)
        return ShortcutUtils.getOrderedShortcutIds(restoredHostnames, favoriteShortcutIds)
            .filter { it in migratedIds && it in availableShortcutIds }
            .take(shortcutMaxCount)
            .toSet()
    }

    private fun normalizeFavoriteShortcutIds(
        favoriteShortcutIds: Set<String>?,
        enabledShortcutIds: Set<String>?,
        availableShortcutIds: Set<String>,
        restoredHostnames: List<DnsHostnameEntry>
    ): Set<String> {
        val migratedFavorites = normalizeShortcutIdSet(favoriteShortcutIds)
        val migratedEnabled = normalizeShortcutIdSet(enabledShortcutIds)
        return ShortcutUtils.getOrderedShortcutIds(restoredHostnames)
            .filter { it in migratedFavorites && it in migratedEnabled && it in availableShortcutIds }
            .take(ShortcutUtils.MAX_FAVORITE_SHORTCUTS)
            .toSet()
    }

    private fun normalizeShortcutIdSet(shortcutIds: Set<String>?): Set<String> {
        @Suppress("SENSELESS_COMPARISON")
        return ShortcutUtils.migrateLegacyShortcutIds(
            shortcutIds.orEmpty()
                .mapNotNull { shortcutId ->
                    if (shortcutId == null || shortcutId.isBlank()) null else shortcutId
                }
                .toSet()
        )
    }

    private fun normalizeDnsState(value: String?, defaultValue: String): String =
        DnsHostnamePolicy.normalizeDnsState(value, validDnsModes, defaultValue)

    private fun normalizeDetectionMode(value: String?): String =
        value?.takeIf(validDetectionModes::contains) ?: TILE_ONLY_DETECTION

    private fun normalizeDelaySeconds(value: Int): Int =
        value.coerceIn(MIN_AUTO_REVERT_DELAY_SECONDS, MAX_AUTO_REVERT_DELAY_SECONDS)

    private companion object {
        const val MIN_AUTO_REVERT_DELAY_SECONDS = 1
        const val MAX_AUTO_REVERT_DELAY_SECONDS = 86_400
        const val MAX_RESTORED_CUSTOM_HOSTNAMES = 250
    }
}
