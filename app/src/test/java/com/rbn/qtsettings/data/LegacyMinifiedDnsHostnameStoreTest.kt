package com.rbn.qtsettings.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Covers the update path from a minified release that persisted [DnsHostnameEntry] with
 * R8's obfuscated field names into `dns_hostnames_list_v2`.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyMinifiedDnsHostnameStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun load_recoversCustomHostnamesWrittenByMinifiedRelease() {
        writeStoredHostnames(LEGACY_MINIFIED_STORE)

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        val custom = hostnames.firstOrNull { it.id == "custom_legacy" }
        assertNotNull("custom hostname was dropped", custom)
        assertEquals("My DNS", custom!!.name)
        assertEquals("dns.example.com", custom.hostname)
        assertTrue(custom.isSelectedForCycle)
    }

    @Test
    fun load_keepsPredefinedSelectionWrittenByMinifiedRelease() {
        writeStoredHostnames(LEGACY_MINIFIED_STORE)

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        val adguard = hostnames.first { it.id == "adguard_default" }
        assertTrue(adguard.isPredefined)
        assertEquals(false, adguard.isSelectedForCycle)
        assertNotNull(adguard.descriptionResId)
    }

    @Test
    fun load_rewritesStoreWithStableFieldNames() {
        writeStoredHostnames(LEGACY_MINIFIED_STORE)

        PreferencesManager.getInstance(context)

        val rewritten = readStoredHostnames()
        assertTrue("store was not upgraded", rewritten.contains("\"hostname\""))
        assertTrue(rewritten.contains("dns.example.com"))
    }

    @Test
    fun load_leavesCurrentFormatUntouched() {
        writeStoredHostnames(
            """[{"id":"custom_current","name":"Current DNS","hostname":"current.example.com",
               "isPredefined":false,"isSelectedForCycle":true}]"""
        )

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        assertNotNull(hostnames.firstOrNull { it.hostname == "current.example.com" })
    }

    @Test
    fun load_keepsEntriesAddedOnVersion141() {
        writeStoredHostnames(VERSION_141_STORE)

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        val added = hostnames.firstOrNull { it.id == "added_on_141" }
        assertNotNull("hostname added on 1.4.1 was dropped", added)
        assertEquals("Added on 1.4.1", added!!.name)
        assertEquals("added.example.com", added.hostname)

        val quad9 = hostnames.first { it.id == "quad9_default" }
        assertEquals(false, quad9.isSelectedForCycle)
    }

    @Test
    fun load_handlesStoreMixingBothFormats() {
        writeStoredHostnames(
            """[
              {"a":"custom_legacy","b":"My DNS","c":"dns.example.com","d":false,"e":true},
              {"id":"added_on_141","name":"Added on 1.4.1","hostname":"added.example.com",
               "isPredefined":false,"isSelectedForCycle":true}
            ]"""
        )

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        assertNotNull(hostnames.firstOrNull { it.hostname == "dns.example.com" })
        assertNotNull(hostnames.firstOrNull { it.hostname == "added.example.com" })
    }

    @Test
    fun load_survivesUnparseableStore() {
        writeStoredHostnames("not json at all")

        val hostnames = PreferencesManager.getInstance(context).dnsHostnames.value

        assertEquals(DnsHostnamePolicy.defaultHostnames().size, hostnames.size)
    }

    private fun writeStoredHostnames(json: String) {
        prefs().edit().putString("dns_hostnames_list_v2", json).commit()
    }

    private fun readStoredHostnames(): String =
        prefs().getString("dns_hostnames_list_v2", "").orEmpty()

    private fun prefs() = context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)

    private fun clearPreferences() {
        prefs().edit().clear().commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private companion object {
        val LEGACY_MINIFIED_STORE = """
            [
              {"a":"adguard_default","b":"AdGuard DNS","c":"dns.adguard.com","d":true,"e":false,"f":2131623936},
              {"a":"cloudflare_default","b":"Cloudflare (1.1.1.1)","c":"one.one.one.one","d":true,"e":true,"f":2131623937},
              {"a":"quad9_default","b":"Quad9 Security","c":"dns.quad9.net","d":true,"e":true,"f":2131623938},
              {"a":"custom_legacy","b":"My DNS","c":"dns.example.com","d":false,"e":true}
            ]
        """.trimIndent()

        /** What 1.4.1 leaves behind: stable field names, defaults only, plus anything re-added there. */
        val VERSION_141_STORE = """
            [
              {"id":"adguard_default","name":"AdGuard DNS","hostname":"dns.adguard.com",
               "isPredefined":true,"isSelectedForCycle":true,"descriptionResId":2131623936},
              {"id":"cloudflare_default","name":"Cloudflare (1.1.1.1)","hostname":"one.one.one.one",
               "isPredefined":true,"isSelectedForCycle":true,"descriptionResId":2131623937},
              {"id":"quad9_default","name":"Quad9 Security","hostname":"dns.quad9.net",
               "isPredefined":true,"isSelectedForCycle":false,"descriptionResId":2131623938},
              {"id":"added_on_141","name":"Added on 1.4.1","hostname":"added.example.com",
               "isPredefined":false,"isSelectedForCycle":true}
            ]
        """.trimIndent()
    }
}
