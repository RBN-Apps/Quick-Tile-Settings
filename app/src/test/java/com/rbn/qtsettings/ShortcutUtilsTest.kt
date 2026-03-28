package com.rbn.qtsettings

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.ShortcutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutUtilsTest {

    // === getShortcutIdForDnsEntry ===

    @Test
    fun getShortcutIdForDnsEntry_predefinedAdGuard_returnsAdGuardId() {
        val entry = DnsHostnameEntry(
            id = "adguard_default",
            name = "AdGuard DNS",
            hostname = "dns.adguard.com",
            isPredefined = true
        )
        assertEquals(
            ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutUtils.getShortcutIdForDnsEntry(entry)
        )
    }

    @Test
    fun getShortcutIdForDnsEntry_predefinedCloudflare_returnsCloudflareId() {
        val entry = DnsHostnameEntry(
            id = "cloudflare_default",
            name = "Cloudflare DNS",
            hostname = "one.one.one.one",
            isPredefined = true
        )
        assertEquals(
            ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutUtils.getShortcutIdForDnsEntry(entry)
        )
    }

    @Test
    fun getShortcutIdForDnsEntry_predefinedQuad9_returnsQuad9Id() {
        val entry = DnsHostnameEntry(
            id = "quad9_default",
            name = "Quad9",
            hostname = "dns.quad9.net",
            isPredefined = true
        )
        assertEquals(
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
            ShortcutUtils.getShortcutIdForDnsEntry(entry)
        )
    }

    @Test
    fun getShortcutIdForDnsEntry_predefinedWithUnmappedId_fallsBackToCustomFormat() {
        val uuid = "some-unknown-predefined-id"
        val entry = DnsHostnameEntry(
            id = uuid,
            name = "Unknown Provider",
            hostname = "unknown.example.com",
            isPredefined = true
        )
        assertEquals("dns_custom_$uuid", ShortcutUtils.getShortcutIdForDnsEntry(entry))
    }

    @Test
    fun getShortcutIdForDnsEntry_customEntry_returnsCustomFormatId() {
        val uuid = "abc-123-def"
        val entry = DnsHostnameEntry(
            id = uuid,
            name = "My DNS",
            hostname = "my.dns.com",
            isPredefined = false
        )
        assertEquals("dns_custom_$uuid", ShortcutUtils.getShortcutIdForDnsEntry(entry))
    }

    @Test
    fun getShortcutIdForAction_builtInAction_returnsMatchingShortcutId() {
        assertEquals(
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.getShortcutIdForAction("com.rbn.qtsettings.action.DNS_AUTO")
        )
    }

    @Test
    fun getShortcutIdForAction_unknownAction_returnsNull() {
        assertEquals(null, ShortcutUtils.getShortcutIdForAction("unknown"))
    }

    @Test
    fun getCustomEntryIdFromShortcutId_customShortcut_returnsEntryId() {
        assertEquals(
            "abc-123",
            ShortcutUtils.getCustomEntryIdFromShortcutId("dns_custom_abc-123")
        )
    }

    @Test
    fun getCustomEntryIdFromShortcutId_builtInShortcut_returnsNull() {
        assertEquals(
            null,
            ShortcutUtils.getCustomEntryIdFromShortcutId(ShortcutUtils.SHORTCUT_ID_DNS_AUTO)
        )
    }

    // === migrateLegacyShortcutIds ===

    @Test
    fun migrateLegacyShortcutIds_allSevenLegacyIds_mappedToNewFormat() {
        val legacyIds = setOf(
            "dns_off",
            "dns_auto",
            "dns_adguard",
            "dns_cloudflare",
            "dns_quad9",
            "usb_on",
            "usb_off"
        )
        val expected = setOf(
            ShortcutUtils.SHORTCUT_ID_DNS_OFF,
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
            ShortcutUtils.SHORTCUT_ID_USB_ON,
            ShortcutUtils.SHORTCUT_ID_USB_OFF
        )
        assertEquals(expected, ShortcutUtils.migrateLegacyShortcutIds(legacyIds))
    }

    @Test
    fun migrateLegacyShortcutIds_alreadyNewFormatIds_returnedUnchanged() {
        val newIds = setOf(ShortcutUtils.SHORTCUT_ID_DNS_OFF, ShortcutUtils.SHORTCUT_ID_USB_ON)
        assertEquals(newIds, ShortcutUtils.migrateLegacyShortcutIds(newIds))
    }

    @Test
    fun migrateLegacyShortcutIds_unknownIds_returnedUnchanged() {
        val unknownIds = setOf("custom_something", "dns_custom_abc123")
        assertEquals(unknownIds, ShortcutUtils.migrateLegacyShortcutIds(unknownIds))
    }

    @Test
    fun migrateLegacyShortcutIds_mixedInput_convertsOnlyLegacyIds() {
        val mixed = setOf("dns_off", ShortcutUtils.SHORTCUT_ID_USB_ON, "dns_custom_abc")
        val result = ShortcutUtils.migrateLegacyShortcutIds(mixed)

        assertTrue(
            "Legacy 'dns_off' must be migrated",
            result.contains(ShortcutUtils.SHORTCUT_ID_DNS_OFF)
        )
        assertTrue(
            "New-format USB On must be preserved",
            result.contains(ShortcutUtils.SHORTCUT_ID_USB_ON)
        )
        assertTrue("Custom ID must be preserved unchanged", result.contains("dns_custom_abc"))
        assertFalse("Legacy 'dns_off' must not remain in result", result.contains("dns_off"))
    }

    @Test
    fun migrateLegacyShortcutIds_emptySet_returnsEmptySet() {
        assertTrue(ShortcutUtils.migrateLegacyShortcutIds(emptySet()).isEmpty())
    }

    // === getAvailableShortcutIds ===

    @Test
    fun getAvailableShortcutIds_emptyHostnames_containsAllSevenBuiltIns() {
        val result = ShortcutUtils.getAvailableShortcutIds(emptyList())
        val expected = setOf(
            ShortcutUtils.SHORTCUT_ID_DNS_OFF,
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
            ShortcutUtils.SHORTCUT_ID_USB_ON,
            ShortcutUtils.SHORTCUT_ID_USB_OFF
        )
        assertEquals(expected, result)
    }

    @Test
    fun getAvailableShortcutIds_predefinedHostnamesOnly_noNewIdsAdded() {
        // Predefined entries map to IDs already in the built-in set -> no new IDs
        val predefined = listOf(
            DnsHostnameEntry(
                id = "adguard_default",
                name = "AdGuard",
                hostname = "dns.adguard.com",
                isPredefined = true
            ),
            DnsHostnameEntry(
                id = "cloudflare_default",
                name = "Cloudflare",
                hostname = "one.one.one.one",
                isPredefined = true
            )
        )
        val result = ShortcutUtils.getAvailableShortcutIds(predefined)
        assertEquals("Predefined entries must not grow the ID set beyond 7", 7, result.size)
    }

    @Test
    fun getAvailableShortcutIds_singleCustomHostname_addsOneCustomId() {
        val uuid = "my-custom-uuid"
        val custom = listOf(
            DnsHostnameEntry(
                id = uuid,
                name = "My DNS",
                hostname = "my.dns.com",
                isPredefined = false
            )
        )
        val result = ShortcutUtils.getAvailableShortcutIds(custom)

        assertEquals("Expected 8 IDs (7 built-in + 1 custom)", 8, result.size)
        assertTrue(
            "Custom ID 'dns_custom_$uuid' must be present",
            result.contains("dns_custom_$uuid")
        )
    }

    @Test
    fun getAvailableShortcutIds_multipleCustomHostnames_addsAllCustomIds() {
        val customs = (1..3).map { i ->
            DnsHostnameEntry(
                id = "uuid-$i",
                name = "DNS $i",
                hostname = "dns$i.example.com",
                isPredefined = false
            )
        }
        val result = ShortcutUtils.getAvailableShortcutIds(customs)

        assertEquals("Expected 10 IDs (7 built-in + 3 custom)", 10, result.size)
        customs.forEach { entry ->
            assertTrue(
                "Custom ID for '${entry.name}' must be present",
                result.contains("dns_custom_${entry.id}")
            )
        }
    }

    // === getOrderedShortcutIds ===

    @Test
    fun getOrderedShortcutIds_emptyHostnames_returnsBuiltInsInCorrectOrder() {
        val result = ShortcutUtils.getOrderedShortcutIds(emptyList())
        val expectedOrder = listOf(
            ShortcutUtils.SHORTCUT_ID_DNS_OFF,
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
            ShortcutUtils.SHORTCUT_ID_USB_ON,
            ShortcutUtils.SHORTCUT_ID_USB_OFF
        )
        assertEquals(expectedOrder, result)
    }

    @Test
    fun getOrderedShortcutIds_withCustomHostname_customEntryAppendedAfterBuiltIns() {
        val custom = DnsHostnameEntry(
            id = "abc",
            name = "My DNS",
            hostname = "my.dns.com",
            isPredefined = false
        )
        val result = ShortcutUtils.getOrderedShortcutIds(listOf(custom))

        val usbOffIndex = result.indexOf(ShortcutUtils.SHORTCUT_ID_USB_OFF)
        val customIndex = result.indexOf("dns_custom_abc")

        assertTrue(
            "Custom entry must appear after all built-in shortcuts",
            usbOffIndex < customIndex
        )
        assertEquals("Total entries: 7 built-in + 1 custom", 8, result.size)
    }

    @Test
    fun getOrderedShortcutIds_multipleCustomHostnames_sortedCaseInsensitively() {
        val customs = listOf(
            DnsHostnameEntry(
                id = "z",
                name = "Zebra DNS",
                hostname = "z.example.com",
                isPredefined = false
            ),
            DnsHostnameEntry(
                id = "a",
                name = "alpha dns",
                hostname = "a.example.com",
                isPredefined = false
            ),
            DnsHostnameEntry(
                id = "m",
                name = "Middle DNS",
                hostname = "m.example.com",
                isPredefined = false
            )
        )
        val result = ShortcutUtils.getOrderedShortcutIds(customs)
        val customPart = result.drop(7) // skip the 7 built-ins

        assertEquals(
            "Custom entries must be sorted by name case-insensitively",
            listOf("dns_custom_a", "dns_custom_m", "dns_custom_z"),
            customPart
        )
    }

    @Test
    fun getOrderedShortcutIds_predefinedHostnames_notDuplicated() {
        // Predefined entries are filtered out of the custom section -> no duplication with built-ins
        val predefined = listOf(
            DnsHostnameEntry(
                id = "adguard_default",
                name = "AdGuard",
                hostname = "dns.adguard.com",
                isPredefined = true
            )
        )
        val result = ShortcutUtils.getOrderedShortcutIds(predefined)

        assertEquals(
            "Predefined entries must not duplicate built-in IDs",
            1,
            result.count { it == ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD }
        )
        assertEquals("Total must still be 7", 7, result.size)
    }

    @Test
    fun getOrderedShortcutIds_withFavorites_favoritesAreMovedToFrontInCanonicalOrder() {
        val custom = listOf(
            DnsHostnameEntry(
                id = "custom-a",
                name = "Alpha DNS",
                hostname = "a.example.com",
                isPredefined = false
            ),
            DnsHostnameEntry(
                id = "custom-z",
                name = "Zeta DNS",
                hostname = "z.example.com",
                isPredefined = false
            )
        )
        val favorites = setOf(
            "dns_custom_custom-z",
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.SHORTCUT_ID_USB_OFF
        )

        val result = ShortcutUtils.getOrderedShortcutIds(custom, favorites)

        assertEquals(
            listOf(
                ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
                ShortcutUtils.SHORTCUT_ID_USB_OFF,
                "dns_custom_custom-z"
            ),
            result.take(3)
        )
    }

    @Test
    fun getOrderedShortcutIds_withUnknownFavoriteIds_ignoresUnknownIds() {
        val result = ShortcutUtils.getOrderedShortcutIds(
            hostnames = emptyList(),
            favoriteShortcutIds = setOf("unknown_shortcut_id", ShortcutUtils.SHORTCUT_ID_DNS_AUTO)
        )

        assertEquals(ShortcutUtils.SHORTCUT_ID_DNS_AUTO, result.first())
        assertFalse(result.contains("unknown_shortcut_id"))
    }
}
