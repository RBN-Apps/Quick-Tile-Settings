package com.rbn.qtsettings.data

import com.rbn.qtsettings.R
import com.rbn.qtsettings.utils.DnsHostnameValidator
import java.util.Locale
import java.util.UUID

internal object DnsHostnamePolicy {
    private const val MAX_HOSTNAME_DISPLAY_NAME_LENGTH = 80

    fun sort(
        hostnames: List<DnsHostnameEntry>,
        mode: DnsListSortMode
    ): List<DnsHostnameEntry> = when (mode) {
        DnsListSortMode.MANUAL -> hostnames.toList()
        DnsListSortMode.ALPHABETICAL -> hostnames.sortedWith(
            compareBy<DnsHostnameEntry> { alphabeticalSortKey(it.name) }
                .thenBy { alphabeticalSortKey(it.hostname) }
                .thenBy { safeString(it.id) }
        )
    }

    fun reconcile(
        storedHostnames: List<DnsHostnameEntry>?,
        normalizeCustomEntries: Boolean,
        maxCustomEntries: Int = Int.MAX_VALUE
    ): List<DnsHostnameEntry> {
        val defaults = defaultHostnames()
        val defaultsById = defaults.associateBy { it.id }
        val seenIds = mutableSetOf<String>()
        var customEntryCount = 0
        val reconciled = mutableListOf<DnsHostnameEntry>()

        storedHostnames.orEmpty().forEach { storedEntry ->
            if (storedEntry.isPredefined) {
                val defaultEntry = defaultsById[storedEntry.id] ?: return@forEach
                if (!seenIds.add(defaultEntry.id)) return@forEach

                reconciled += defaultEntry.copy(
                    isSelectedForCycle = storedEntry.isSelectedForCycle
                )
                return@forEach
            }

            val storedId = nullableAfterGsonRestore(storedEntry.id)
            val storedName = nullableAfterGsonRestore(storedEntry.name)
            val storedHostname = nullableAfterGsonRestore(storedEntry.hostname)
            if (storedName == null || storedHostname == null || customEntryCount >= maxCustomEntries) {
                return@forEach
            }

            if (normalizeCustomEntries && (
                    storedId.isNullOrBlank() ||
                        storedName.isBlank() ||
                        storedId in defaultsById ||
                        storedId in seenIds ||
                        !DnsHostnameValidator.isValid(storedHostname)
                    )
            ) return@forEach

            val reconciledId = if (
                storedId.isNullOrBlank() || storedId in defaultsById || storedId in seenIds
            ) {
                generateUniqueCustomId(defaultsById.keys + seenIds)
            } else {
                storedId
            }
            val reconciledName = if (!normalizeCustomEntries && storedName.isBlank()) {
                storedHostname.takeIf { it.isNotBlank() } ?: reconciledId
            } else {
                storedName
            }

            seenIds += reconciledId
            customEntryCount++
            reconciled += if (normalizeCustomEntries) {
                storedEntry.copy(
                    id = reconciledId,
                    name = reconciledName.trim().take(MAX_HOSTNAME_DISPLAY_NAME_LENGTH),
                    hostname = storedHostname.trim().lowercase(Locale.ROOT),
                    isPredefined = false,
                    descriptionResId = null
                )
            } else {
                storedEntry.copy(
                    id = reconciledId,
                    name = reconciledName,
                    isPredefined = false,
                    descriptionResId = null
                )
            }
        }

        defaults.forEach { defaultEntry ->
            if (seenIds.add(defaultEntry.id)) {
                reconciled += defaultEntry
            }
        }
        return reconciled
    }

    fun defaultHostnames(): List<DnsHostnameEntry> = listOf(
        DnsHostnameEntry(
            id = "adguard_default",
            name = "AdGuard DNS",
            hostname = "dns.adguard.com",
            isPredefined = true,
            isSelectedForCycle = true,
            descriptionResId = R.string.dns_info_adguard
        ),
        DnsHostnameEntry(
            id = "cloudflare_default",
            name = "Cloudflare (1.1.1.1)",
            hostname = "one.one.one.one",
            isPredefined = true,
            isSelectedForCycle = true,
            descriptionResId = R.string.dns_info_cloudflare
        ),
        DnsHostnameEntry(
            id = "quad9_default",
            name = "Quad9 Security",
            hostname = "dns.quad9.net",
            isPredefined = true,
            isSelectedForCycle = true,
            descriptionResId = R.string.dns_info_quad9
        )
    )

    fun normalizeDnsState(value: String?, validModes: Set<String>, defaultValue: String): String =
        value?.takeIf(validModes::contains) ?: defaultValue

    fun normalizeStateHostname(
        state: String,
        hostname: String?,
        availableHostnames: Set<String>,
        dnsModeOn: String
    ): String? {
        val normalizedHostname = hostname?.trim()?.lowercase(Locale.ROOT)
        return normalizedHostname?.takeIf {
            state == dnsModeOn && it in availableHostnames
        }
    }

    private fun alphabeticalSortKey(value: String?): String =
        value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    private fun safeString(value: String?): String = value.orEmpty()

    private fun <T> nullableAfterGsonRestore(value: T): T? = value

    private fun generateUniqueCustomId(unavailableIds: Set<String>): String =
        generateSequence { UUID.randomUUID().toString() }
            .first { it !in unavailableIds }
}
