package com.rbn.qtsettings.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.rbn.qtsettings.utils.ShortcutUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DnsListSortingTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private val gson = Gson()
    private val hostnameListType = object : TypeToken<List<DnsHostnameEntry>>() {}.type

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearPreferences()
        resetPreferencesManagerSingleton()
        preferencesManager = PreferencesManager.getInstance(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun freshInstall_defaultsToAlphabeticalAndUsesDeterministicTieBreakersAcrossAllEntries() {
        recreateManagerWithStoredEntries(
            entries = listOf(
                customEntry(id = "custom-c", name = "same", hostname = "z.example.com"),
                customEntry(id = "custom-b", name = "Same", hostname = "a.example.com"),
                customEntry(id = "custom-a", name = "SAME", hostname = "a.example.com")
            ),
            sortMode = null
        )

        assertEquals(DnsListSortMode.ALPHABETICAL, preferencesManager.dnsListSortMode.value)
        assertEquals(
            listOf("custom-a", "custom-b", "custom-c"),
            preferencesManager.dnsHostnames.value
                .filterNot { it.isPredefined }
                .map { it.id }
        )
        assertEquals(
            DnsListSortMode.ALPHABETICAL,
            preferencesManager.getDnsListSortModeBlocking()
        )
    }

    @Test
    fun reorder_isRejectedInAlphabeticalMode_andAlphaToManualRestoresCanonicalOrder() {
        val quad9Id = "quad9_default"
        val quad9First = orderWithEntryAt(quad9Id, 0)

        assertFalse(preferencesManager.reorderDnsHostnames(quad9First))

        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        assertTrue(preferencesManager.reorderDnsHostnames(quad9First))
        assertEquals(quad9Id, preferencesManager.dnsHostnames.value.first().id)
        assertEquals(quad9Id, readPersistedManualEntries().first().id)

        preferencesManager.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)
        assertEquals("adguard_default", preferencesManager.dnsHostnames.value.first().id)

        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        assertEquals(quad9Id, preferencesManager.dnsHostnames.value.first().id)
    }

    @Test
    fun reorder_commitsWholeManualOrderOnce_andRejectsIncompleteOrDuplicateIds() {
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        val originalIds = preferencesManager.dnsHostnames.value.map { it.id }
        val reversedIds = originalIds.reversed()

        assertTrue(preferencesManager.reorderDnsHostnames(reversedIds))
        assertEquals(reversedIds, preferencesManager.dnsHostnames.value.map { it.id })
        assertEquals(reversedIds, readPersistedManualEntries().map { it.id })

        assertFalse(preferencesManager.reorderDnsHostnames(reversedIds.dropLast(1)))
        assertFalse(
            preferencesManager.reorderDnsHostnames(
                reversedIds.dropLast(1) + reversedIds.first()
            )
        )
        assertEquals(reversedIds, preferencesManager.dnsHostnames.value.map { it.id })
    }

    @Test
    fun editAddAndDeleteWhileAlphabetical_preserveCanonicalPositionsAndAppendNewEntries() {
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        preferencesManager.addCustomDnsHostname("Zulu", "z.example.com")
        preferencesManager.addCustomDnsHostname("Alpha", "a.example.com")
        val zulu = preferencesManager.dnsHostnames.value.first { it.name == "Zulu" }
        val alpha = preferencesManager.dnsHostnames.value.first { it.name == "Alpha" }
        val zuluManualIndex = readPersistedManualEntries().indexOfFirst { it.id == zulu.id }
        val alphaManualIndex = readPersistedManualEntries().indexOfFirst { it.id == alpha.id }

        preferencesManager.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)
        preferencesManager.updateDnsHostnameEntry(zulu.copy(name = "000 First"))

        assertEquals(zulu.id, preferencesManager.dnsHostnames.value.first().id)
        assertEquals(
            zuluManualIndex,
            readPersistedManualEntries().indexOfFirst { it.id == zulu.id }
        )
        assertEquals(
            alphaManualIndex,
            readPersistedManualEntries().indexOfFirst { it.id == alpha.id }
        )

        preferencesManager.deleteCustomDnsHostname(zulu.id)
        preferencesManager.addCustomDnsHostname("Beta", "b.example.com")
        val beta = preferencesManager.dnsHostnames.value.first { it.name == "Beta" }

        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        val manualIds = preferencesManager.dnsHostnames.value.map { it.id }
        assertFalse(manualIds.contains(zulu.id))
        assertTrue(manualIds.indexOf(alpha.id) < manualIds.indexOf(beta.id))
        assertEquals(beta.id, manualIds.last())
    }

    @Test
    fun manualOrderAndMode_surviveManagerRecreation() {
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        assertTrue(
            preferencesManager.reorderDnsHostnames(
                orderWithEntryAt("quad9_default", 0)
            )
        )

        resetPreferencesManagerSingleton()
        preferencesManager = PreferencesManager.getInstance(context)

        assertEquals(DnsListSortMode.MANUAL, preferencesManager.dnsListSortMode.value)
        assertEquals("quad9_default", preferencesManager.dnsHostnames.value.first().id)
        assertEquals("quad9_default", readPersistedManualEntries().first().id)
    }

    @Test
    fun cycleAndBlockingReads_followEffectiveMode() {
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        assertTrue(
            preferencesManager.reorderDnsHostnames(
                orderWithEntryAt("quad9_default", 0)
            )
        )

        assertEquals(
            "quad9_default",
            preferencesManager.getDnsHostnamesSelectedForCycle().first().id
        )
        assertEquals("quad9_default", preferencesManager.getAllDnsHostnamesBlocking().first().id)

        preferencesManager.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)

        assertEquals(
            "adguard_default",
            preferencesManager.getDnsHostnamesSelectedForCycle().first().id
        )
        assertEquals("adguard_default", preferencesManager.getAllDnsHostnamesBlocking().first().id)
    }

    @Test
    fun backupRoundTrip_preservesModeAndCanonicalManualOrder() {
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        preferencesManager.addCustomDnsHostname("Custom", "custom.example.com")
        val custom = preferencesManager.dnsHostnames.value.first { !it.isPredefined }
        assertTrue(
            preferencesManager.reorderDnsHostnames(
                orderWithEntryAt(custom.id, 0)
            )
        )
        preferencesManager.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)

        val backupJson = preferencesManager.exportSettingsBackupJson()
        val backup = gson.fromJson(backupJson, SettingsBackup::class.java)
        assertEquals(DnsListSortMode.ALPHABETICAL.persistedValue, backup.dns?.sortMode)
        assertEquals(custom.id, backup.dns?.hostnames?.first()?.id)

        preferencesManager.deleteCustomDnsHostname(custom.id)
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        preferencesManager.restoreSettingsBackupJson(backupJson)

        assertEquals(DnsListSortMode.ALPHABETICAL, preferencesManager.dnsListSortMode.value)
        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        assertEquals(custom.id, preferencesManager.dnsHostnames.value.first().id)
    }

    @Test
    fun schemaV1BackupWithoutSortMode_remainsCompatibleAndDefaultsToAlphabetical() {
        val backupObject = JsonParser.parseString(
            preferencesManager.exportSettingsBackupJson()
        ).asJsonObject
        backupObject.getAsJsonObject("dns").remove("sortMode")

        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        preferencesManager.restoreSettingsBackupJson(gson.toJson(backupObject))

        assertEquals(1, backupObject.get("schemaVersion").asInt)
        assertEquals(DnsListSortMode.ALPHABETICAL, preferencesManager.dnsListSortMode.value)
    }

    @Test
    fun load_preservesStoredInterleaving_updatesPredefinedMetadata_andAppendsMissingDefaults() {
        recreateManagerWithStoredEntries(
            entries = listOf(
                customEntry(id = "custom-b", name = "B", hostname = "b.example.com"),
                DnsHostnameEntry(
                    id = "quad9_default",
                    name = "Stale Quad9 Name",
                    hostname = "stale.example.com",
                    isPredefined = true,
                    isSelectedForCycle = false,
                    descriptionResId = null
                ),
                customEntry(id = "custom-a", name = "A", hostname = "a.example.com"),
                DnsHostnameEntry(
                    id = "adguard_default",
                    name = "Stale AdGuard Name",
                    hostname = "stale-adguard.example.com",
                    isPredefined = true,
                    isSelectedForCycle = true,
                    descriptionResId = null
                )
            ),
            sortMode = DnsListSortMode.MANUAL
        )

        val entries = preferencesManager.dnsHostnames.value
        assertEquals(
            listOf(
                "custom-b",
                "quad9_default",
                "custom-a",
                "adguard_default",
                "cloudflare_default"
            ),
            entries.map { it.id }
        )
        val quad9 = entries.first { it.id == "quad9_default" }
        assertEquals("Quad9 Security", quad9.name)
        assertEquals("dns.quad9.net", quad9.hostname)
        assertFalse(quad9.isSelectedForCycle)
        assertTrue(quad9.descriptionResId != null)
    }

    @Test
    fun load_repairsInvalidCustomIdsWithoutDroppingStoredEntries() {
        recreateManagerWithStoredEntries(
            entries = listOf(
                customEntry(id = "", name = "Blank ID", hostname = "blank-id.example.com"),
                customEntry(
                    id = "adguard_default",
                    name = "Colliding ID",
                    hostname = "collision.example.com"
                ),
                customEntry(
                    id = "blank-name",
                    name = "",
                    hostname = "blank-name.example.com"
                )
            ),
            sortMode = DnsListSortMode.MANUAL
        )

        val restoredCustomEntries = preferencesManager.dnsHostnames.value
            .filterNot { it.isPredefined }

        assertEquals(
            setOf(
                "blank-id.example.com",
                "collision.example.com",
                "blank-name.example.com"
            ),
            restoredCustomEntries.map { it.hostname }.toSet()
        )
        assertTrue(restoredCustomEntries.all { it.id.isNotBlank() })
        assertEquals(
            restoredCustomEntries.size,
            restoredCustomEntries.map { it.id }.toSet().size
        )
        assertTrue(restoredCustomEntries.none { it.id.endsWith("_default") })
        assertEquals(
            "blank-name.example.com",
            restoredCustomEntries.first { it.hostname == "blank-name.example.com" }.name
        )
        assertEquals(
            restoredCustomEntries,
            readPersistedManualEntries().filterNot { it.isPredefined }
        )
    }

    @Test
    fun shortcutOrdering_respectsEffectiveCustomInputOrder() {
        val entries = listOf(
            customEntry(id = "z", name = "Zulu", hostname = "z.example.com"),
            customEntry(id = "a", name = "Alpha", hostname = "a.example.com")
        )

        val customShortcutIds = ShortcutUtils.getOrderedShortcutIds(entries)
            .filter { it.startsWith("dns_custom_") }

        assertEquals(listOf("dns_custom_z", "dns_custom_a"), customShortcutIds)
    }

    private fun recreateManagerWithStoredEntries(
        entries: List<DnsHostnameEntry>,
        sortMode: DnsListSortMode?
    ) {
        resetPreferencesManagerSingleton()
        val editor = preferences().edit()
            .clear()
            .putString(KEY_DNS_HOSTNAMES, gson.toJson(entries))
        if (sortMode != null) {
            editor.putString(KEY_DNS_LIST_SORT_MODE, sortMode.persistedValue)
        }
        editor.commit()
        preferencesManager = PreferencesManager.getInstance(context)
    }

    private fun readPersistedManualEntries(): List<DnsHostnameEntry> {
        val json = preferences().getString(KEY_DNS_HOSTNAMES, null) ?: return emptyList()
        return gson.fromJson(json, hostnameListType)
    }

    private fun orderWithEntryAt(entryId: String, targetIndex: Int): List<String> {
        val orderedIds = preferencesManager.dnsHostnames.value.map { it.id }.toMutableList()
        val sourceIndex = orderedIds.indexOf(entryId)
        require(sourceIndex >= 0) { "Unknown DNS entry ID: $entryId" }
        orderedIds.add(targetIndex, orderedIds.removeAt(sourceIndex))
        return orderedIds
    }

    private fun customEntry(id: String, name: String, hostname: String) = DnsHostnameEntry(
        id = id,
        name = name,
        hostname = hostname,
        isPredefined = false,
        isSelectedForCycle = true
    )

    private fun preferences() =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun clearPreferences() {
        if (::context.isInitialized) {
            preferences().edit().clear().commit()
        }
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    companion object {
        private const val PREFERENCES_NAME = "qt_settings_prefs"
        private const val KEY_DNS_HOSTNAMES = "dns_hostnames_list_v2"
        private const val KEY_DNS_LIST_SORT_MODE = "dns_list_sort_mode_v1"
    }
}
