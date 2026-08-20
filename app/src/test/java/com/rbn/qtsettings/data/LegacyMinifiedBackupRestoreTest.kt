package com.rbn.qtsettings.data

import android.content.Context
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LegacyMinifiedBackupRestoreTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        preferencesManager = PreferencesManager.getInstance(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun restoreSettingsBackupJson_importsMinifiedVersion130Backup() {
        preferencesManager.restoreSettingsBackupJson(LEGACY_1_3_BACKUP)

        assertRestoredCommonSettings(
            customId = "legacy13",
            expectedName = "Legacy 1.3 DNS",
            expectedHostname = "legacy13.example.com",
            expectedDelay = 17,
            expectedUsbDelay = 23
        )
        assertEquals(DnsListSortMode.ALPHABETICAL, preferencesManager.dnsListSortMode.value)
        assertFalse(preferencesManager.dnsToggleAuto.value)
        assertEquals(DNS_MODE_ON, preferencesManager.dnsStateOnWifi.value)
        assertEquals("legacy13.example.com", preferencesManager.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_AUTO, preferencesManager.dnsStateOnMobile.value)
        assertNull(preferencesManager.dnsHostnameOnMobile.value)
    }

    @Test
    fun restoreSettingsBackupJson_importsMinifiedVersion140Backup() {
        preferencesManager.restoreSettingsBackupJson(LEGACY_1_4_BACKUP)

        assertRestoredCommonSettings(
            customId = "legacy14",
            expectedName = "Legacy 1.4 DNS",
            expectedHostname = "legacy14.example.com",
            expectedDelay = 18,
            expectedUsbDelay = 24
        )
        assertEquals(DnsListSortMode.MANUAL, preferencesManager.dnsListSortMode.value)
        assertTrue(preferencesManager.dnsToggleAuto.value)
        assertEquals(DNS_MODE_OFF, preferencesManager.dnsStateOnWifi.value)
        assertNull(preferencesManager.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_ON, preferencesManager.dnsStateOnMobile.value)
        assertEquals("legacy14.example.com", preferencesManager.dnsHostnameOnMobile.value)
    }

    private fun assertRestoredCommonSettings(
        customId: String,
        expectedName: String,
        expectedHostname: String,
        expectedDelay: Int,
        expectedUsbDelay: Int
    ) {
        val restoredEntry = preferencesManager.dnsHostnames.value
            .firstOrNull { it.id == customId }
        assertNotNull(restoredEntry)
        assertEquals(expectedName, restoredEntry?.name)
        assertEquals(expectedHostname, restoredEntry?.hostname)
        assertFalse(restoredEntry?.isSelectedForCycle ?: true)

        assertFalse(preferencesManager.dnsToggleOff.value)
        assertTrue(preferencesManager.dnsEnableAutoRevert.value)
        assertEquals(expectedDelay, preferencesManager.dnsAutoRevertDelaySeconds.value)
        assertTrue(preferencesManager.dnsRequireUnlock.value)
        assertTrue(preferencesManager.vpnDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, preferencesManager.vpnDetectionMode.value)
        assertTrue(preferencesManager.networkTypeDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, preferencesManager.networkTypeDetectionMode.value)

        assertFalse(preferencesManager.usbToggleEnable.value)
        assertTrue(preferencesManager.usbToggleDisable.value)
        assertTrue(preferencesManager.usbAlsoHideDevOptions.value)
        assertTrue(preferencesManager.usbAlsoDisableWirelessDebugging.value)
        assertTrue(preferencesManager.usbEnableAutoRevert.value)
        assertEquals(expectedUsbDelay, preferencesManager.usbAutoRevertDelaySeconds.value)
        assertTrue(preferencesManager.usbRequireUnlock.value)

        val shortcutId = "dns_custom_$customId"
        assertEquals(setOf(shortcutId), preferencesManager.enabledShortcutIds.value)
        assertEquals(setOf(shortcutId), preferencesManager.favoriteShortcutIds.value)
        assertTrue(preferencesManager.allowPinnedShortcutsWhenDisabled.value)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private companion object {
        val LEGACY_1_3_BACKUP = """
            {
              "a": 1680000000000,
              "b": "2023-03-28T10:40:00Z",
              "c": {
                "a": false,
                "b": false,
                "c": [{
                  "a": "legacy13",
                  "b": "Legacy 1.3 DNS",
                  "c": "legacy13.example.com",
                  "d": false,
                  "e": false
                }],
                "d": true,
                "e": 17,
                "f": true,
                "g": true,
                "h": "background",
                "i": true,
                "j": "background",
                "k": "hostname",
                "l": "legacy13.example.com",
                "m": "opportunistic"
              },
              "d": {
                "a": false,
                "b": true,
                "c": true,
                "d": true,
                "e": true,
                "f": 23,
                "g": true
              },
              "e": {
                "a": ["dns_custom_legacy13"],
                "b": ["dns_custom_legacy13"],
                "c": true
              },
              "f": {}
            }
        """.trimIndent()

        val LEGACY_1_4_BACKUP = """
            {
              "a": 1690000000000,
              "b": "2023-07-22T04:26:40Z",
              "c": {
                "a": false,
                "b": true,
                "c": [{
                  "a": "legacy14",
                  "b": "Legacy 1.4 DNS",
                  "c": "legacy14.example.com",
                  "d": false,
                  "e": false
                }],
                "d": "manual",
                "e": true,
                "f": 18,
                "g": true,
                "h": true,
                "i": "background",
                "j": true,
                "k": "background",
                "l": "off",
                "n": "hostname",
                "o": "legacy14.example.com"
              },
              "d": {
                "a": false,
                "b": true,
                "c": true,
                "d": true,
                "e": true,
                "f": 24,
                "g": true
              },
              "e": {
                "a": ["dns_custom_legacy14"],
                "b": ["dns_custom_legacy14"],
                "c": true
              },
              "f": {}
            }
        """.trimIndent()
    }
}
