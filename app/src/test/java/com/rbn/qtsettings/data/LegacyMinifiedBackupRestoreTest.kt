package com.rbn.qtsettings.data

import com.rbn.qtsettings.app.AppStores
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
    private lateinit var preferencesManager: AppStores

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetAppStoresSingleton()
        clearPreferences()
        preferencesManager = AppStores.getInstance(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetAppStoresSingleton()
    }

    @Test
    fun restoreSettingsBackupJson_importsMinifiedVersion130Backup() {
        preferencesManager.backup.restoreSettingsBackupJson(LEGACY_1_3_BACKUP)

        assertRestoredCommonSettings(
            customId = "legacy13",
            expectedName = "Legacy 1.3 DNS",
            expectedHostname = "legacy13.example.com",
            expectedDelay = 17,
            expectedUsbDelay = 23
        )
        assertEquals(DnsListSortMode.ALPHABETICAL, preferencesManager.dns.dnsListSortMode.value)
        assertFalse(preferencesManager.dns.dnsToggleAuto.value)
        assertEquals(DNS_MODE_ON, preferencesManager.automation.dnsStateOnWifi.value)
        assertEquals("legacy13.example.com", preferencesManager.automation.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_AUTO, preferencesManager.automation.dnsStateOnMobile.value)
        assertNull(preferencesManager.automation.dnsHostnameOnMobile.value)
    }

    @Test
    fun restoreSettingsBackupJson_importsMinifiedVersion140Backup() {
        preferencesManager.backup.restoreSettingsBackupJson(LEGACY_1_4_BACKUP)

        assertRestoredCommonSettings(
            customId = "legacy14",
            expectedName = "Legacy 1.4 DNS",
            expectedHostname = "legacy14.example.com",
            expectedDelay = 18,
            expectedUsbDelay = 24
        )
        assertEquals(DnsListSortMode.MANUAL, preferencesManager.dns.dnsListSortMode.value)
        assertTrue(preferencesManager.dns.dnsToggleAuto.value)
        assertEquals(DNS_MODE_OFF, preferencesManager.automation.dnsStateOnWifi.value)
        assertNull(preferencesManager.automation.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_ON, preferencesManager.automation.dnsStateOnMobile.value)
        assertEquals("legacy14.example.com", preferencesManager.automation.dnsHostnameOnMobile.value)
    }

    private fun assertRestoredCommonSettings(
        customId: String,
        expectedName: String,
        expectedHostname: String,
        expectedDelay: Int,
        expectedUsbDelay: Int
    ) {
        val restoredEntry = preferencesManager.dns.dnsHostnames.value
            .firstOrNull { it.id == customId }
        assertNotNull(restoredEntry)
        assertEquals(expectedName, restoredEntry?.name)
        assertEquals(expectedHostname, restoredEntry?.hostname)
        assertFalse(restoredEntry?.isSelectedForCycle ?: true)

        assertFalse(preferencesManager.dns.dnsToggleOff.value)
        assertTrue(preferencesManager.dns.dnsEnableAutoRevert.value)
        assertEquals(expectedDelay, preferencesManager.dns.dnsAutoRevertDelaySeconds.value)
        assertTrue(preferencesManager.dns.dnsRequireUnlock.value)
        assertTrue(preferencesManager.automation.vpnDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, preferencesManager.automation.vpnDetectionMode.value)
        assertTrue(preferencesManager.automation.networkTypeDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, preferencesManager.automation.networkTypeDetectionMode.value)

        assertFalse(preferencesManager.usb.state.value.toggleEnable)
        assertTrue(preferencesManager.usb.state.value.toggleDisable)
        assertTrue(preferencesManager.usb.state.value.alsoHideDevOptions)
        assertTrue(preferencesManager.usb.state.value.alsoDisableWirelessDebugging)
        assertTrue(preferencesManager.usb.state.value.enableAutoRevert)
        assertEquals(expectedUsbDelay, preferencesManager.usb.state.value.autoRevertDelaySeconds)
        assertTrue(preferencesManager.usb.state.value.requireUnlock)

        val shortcutId = "dns_custom_$customId"
        assertEquals(setOf(shortcutId), preferencesManager.shortcuts.enabledShortcutIds.value)
        assertEquals(setOf(shortcutId), preferencesManager.shortcuts.favoriteShortcutIds.value)
        assertTrue(preferencesManager.shortcuts.allowPinnedShortcutsWhenDisabled.value)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetAppStoresSingleton() {
        val instanceField = com.rbn.qtsettings.app.AppStores::class.java.getDeclaredField("INSTANCE")
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
