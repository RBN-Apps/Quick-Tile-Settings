package com.rbn.qtsettings.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.SettingsBackup
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsAutomationIntegrationTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager
    private lateinit var automation: AutomationViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        prefsManager = PreferencesManager.getInstance(context)
        automation = AutomationViewModel(prefsManager.automationStore, { com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource(it) })
        automation.setApplicationContext(context)
    }

    @After
    fun tearDown() {
        shadowOf(context.applicationContext as Application).denyPermissions(
            Manifest.permission.WRITE_SECURE_SETTINGS,
            Manifest.permission.POST_NOTIFICATIONS
        )
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun shortcutSettings_whenFreshlyInitialized_thenNoShortcutsEnabledAndPinnedFallbackDisabled() {
        assertTrue(prefsManager.shortcutStore.enabledShortcutIds.value.isEmpty())
        assertTrue(prefsManager.shortcutStore.favoriteShortcutIds.value.isEmpty())
        assertFalse(prefsManager.shortcutStore.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun wifiNetworkRules_explicitRuleCrudPersistsAcrossPreferencesManagerRecreation() {
        automation.setWifiNetworkRulesEnabled(true)

        assertTrue(
            automation.addWifiNetworkRule(
                ssid = "Campus",
                bssid = "aa:bb:cc:dd:ee:ff",
                actionMode = DNS_MODE_ON,
                dnsHostname = "dns.example.com"
            )
        )
        assertFalse(
            automation.addWifiNetworkRule(
                ssid = "Campus",
                bssid = "AA:BB:CC:DD:EE:FF",
                actionMode = DNS_MODE_OFF,
                dnsHostname = null
            )
        )

        val created = automation.wifiNetworkRules.value.single()
        assertEquals("AA:BB:CC:DD:EE:FF", created.bssid)
        assertTrue(
            automation.updateWifiNetworkRule(
                id = created.id,
                ssid = "Campus WiFi",
                bssid = null,
                actionMode = DNS_MODE_AUTO,
                dnsHostname = null
            )
        )

        resetPreferencesManagerSingleton()
        val restoredPreferencesManager = PreferencesManager.getInstance(context)
        val restoredRule = restoredPreferencesManager.getWifiNetworkRules().single()

        assertEquals("Campus WiFi", restoredRule.ssid)
        assertNull(restoredRule.bssid)
        assertEquals(DNS_MODE_AUTO, restoredRule.actionMode)
        assertNull(restoredRule.dnsHostname)
    }

    @Test
    fun wifiNetworkHistory_recordsMostRecentIdentityAndDeduplicatesIt() {
        prefsManager.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "aa:bb:cc:dd:ee:ff"),
            seenAtEpochMillis = 100
        )
        prefsManager.recordKnownWifiNetwork(
            WifiNetworkIdentity("Home", "11:22:33:44:55:66"),
            seenAtEpochMillis = 200
        )
        prefsManager.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
            seenAtEpochMillis = 300
        )

        val history = prefsManager.getKnownWifiNetworks()
        assertEquals(2, history.size)
        assertEquals("Campus", history[0].ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", history[0].bssid)
        assertEquals(300, history[0].lastSeenEpochMillis)
        assertEquals("Home", history[1].ssid)
    }

    @Test
    fun wifiNetworkHistory_emptyStoreIsNotWrittenDuringInitialization() {
        val rawPreferences = context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)

        assertFalse(rawPreferences.contains("known_wifi_networks_v1"))
    }

    @Test
    fun wifiNetworkRules_emptyStoreIsNotWrittenDuringInitialization() {
        val rawPreferences = context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)

        assertFalse(rawPreferences.contains("wifi_network_dns_rules_v1"))
        assertFalse(rawPreferences.contains("wifi_network_rules_enabled_v1"))
    }

    @Test
    @Config(sdk = [32])
    fun setNetworkTypeDetectionEnabled_whenWifiRulesAreActive_forcesBackground() {
        prefsManager.setNetworkTypeDetectionEnabled(false)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        prefsManager.setWifiNetworkRulesEnabled(true)

        automation.setNetworkTypeDetectionEnabled(true)

        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
    }

    @Test
    @Config(sdk = [32])
    fun setWifiNetworkRulesEnabled_whenNetworkDetectionIsEnabled_forcesBackgroundAndShowsStatus() {
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

        automation.setWifiNetworkRulesEnabled(true)

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertEquals(
            context.getString(R.string.wifi_rules_enabled_background_detection),
            automation.networkDetectionStatusMessage.value
        )
    }

    @Test
    fun setNetworkTypeDetectionMode_whenWifiRulesAreActive_rejectsTileOnly() {
        val contextFreeViewModel = AutomationViewModel(prefsManager.automationStore, { com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource(it) })
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setWifiNetworkRulesEnabled(true)

        contextFreeViewModel.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

        assertEquals(BACKGROUND_DETECTION, contextFreeViewModel.networkTypeDetectionMode.value)
    }

    @Test
    @Config(sdk = [33])
    fun setWifiNetworkRulesEnabled_whenNotificationPermissionIsMissing_activatesImmediatelyWithoutPrompt() {
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

        automation.setWifiNetworkRulesEnabled(true)

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun repeatedActivationDoesNotPrompt_butNotificationOptionStillDoes() {
        repeat(3) {
            automation.setNetworkTypeDetectionEnabled(true)
            automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
            automation.setWifiNetworkRulesEnabled(true)
            automation.setVpnDetectionMode(BACKGROUND_DETECTION)
            assertFalse(automation.showNotificationPermissionExplanationDialog.value)
            assertEquals(0, automation.requestNotificationPermission.value)
            automation.setWifiNetworkRulesEnabled(false)
            automation.setNetworkTypeDetectionEnabled(false)
        }
        automation.showOptionalNotificationExplanation()
        assertTrue(automation.showNotificationPermissionExplanationDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun notificationGrantForWifiRule_withoutWriteSecureSettings_doesNotStartForegroundService() {
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        val shadowApplication = shadowOf(context.applicationContext as Application)
        shadowApplication.clearStartedServices()

        automation.setWifiNetworkRulesEnabled(true)
        automation.onNotificationPermissionResult(granted = true)

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertNull(shadowApplication.peekNextStartedService())
    }

    @Test
    @Config(sdk = [32, 33])
    fun backgroundNetworkDetection_withoutNotificationPermission_startsForegroundService() {
        val shadowApplication = shadowOf(context.applicationContext as Application)
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowApplication.clearStartedServices()
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

        automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        val startedService = shadowApplication.peekNextStartedService()
        assertEquals(
            NetworkMonitoringService::class.java.name,
            startedService.component?.className
        )
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun continueWithoutNotifications_whenWifiRulesWereEnabled_keepsRulesAndBackgroundDetection() {
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        automation.setWifiNetworkRulesEnabled(true)
        automation.continueWithoutNotificationPermission()

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun continueWithoutNotifications_whenEnablingDetectionWithActiveRules_keepsRulesEnabled() {
        prefsManager.setNetworkTypeDetectionEnabled(false)
        prefsManager.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        prefsManager.setWifiNetworkRulesEnabled(true)

        automation.setNetworkTypeDetectionEnabled(true)

        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)

        automation.continueWithoutNotificationPermission()

        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertTrue(automation.wifiNetworkRulesEnabled.value)
    }

    @Test
    fun wifiNetworkRules_backupExportAndRestore_roundTripsEnabledStateAndRules() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Home", null, "default", null))
        assertTrue(automation.addWifiNetworkRule("Büro", null, "default", null))
        val backupJson = prefsManager.exportSettingsBackupJson()
        val exportedBackup = Gson().fromJson(backupJson, SettingsBackup::class.java)

        assertTrue(exportedBackup.dns?.wifiNetworkRulesEnabled == true)

        automation.setWifiNetworkRulesEnabled(false)
        automation.wifiNetworkRules.value.forEach { automation.deleteWifiNetworkRule(it.id) }
        assertTrue(automation.addWifiNetworkRule("Temporary", null, DNS_MODE_OFF, null))

        prefsManager.restoreSettingsBackupJson(backupJson)

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(
            setOf("Home", "Büro"),
            automation.wifiNetworkRules.value.mapNotNull { it.ssid }.toSet()
        )
        assertTrue(prefsManager.areWifiNetworkRulesEnabled())
    }

    @Test
    fun wifiNetworkRules_backupRoundTripsExplicitActionsAndHistory() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(
            automation.addWifiNetworkRule(
                ssid = "Campus",
                bssid = "AA:BB:CC:DD:EE:FF",
                actionMode = DNS_MODE_ON,
                dnsHostname = "dns.google"
            )
        )
        prefsManager.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
            seenAtEpochMillis = 1234
        )
        val backupJson = prefsManager.exportSettingsBackupJson()

        automation.deleteWifiNetworkRule(automation.wifiNetworkRules.value.single().id)
        prefsManager.restoreSettingsBackupJson(backupJson)

        val restoredRule = automation.wifiNetworkRules.value.single()
        assertEquals("Campus", restoredRule.ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", restoredRule.bssid)
        assertEquals(DNS_MODE_ON, restoredRule.actionMode)
        assertEquals("dns.google", restoredRule.dnsHostname)
        assertEquals(1234, automation.knownWifiNetworks.value.single().lastSeenEpochMillis)
    }

    @Test
    fun wifiNetworkRules_restoreOldBackupWithoutRuleFields_usesDisabledAndEmptyDefaults() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Existing", null, "default", null))
        val oldBackupJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1,
              "dns": {
                "toggleOff": true,
                "toggleAuto": true,
                "hostnames": [],
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false,
                "vpnDetectionEnabled": false,
                "vpnDetectionMode": "tile_only",
                "networkTypeDetectionEnabled": false,
                "networkTypeDetectionMode": "tile_only",
                "dnsStateOnWifi": "off",
                "dnsHostnameOnWifi": null,
                "dnsStateOnMobile": "opportunistic",
                "dnsHostnameOnMobile": null
              },
              "usb": {
                "toggleEnable": true,
                "toggleDisable": true,
                "alsoHideDevOptions": false,
                "alsoDisableWirelessDebugging": false,
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false
              },
              "shortcuts": {
                "enabledShortcutIds": [],
                "favoriteShortcutIds": [],
                "allowPinnedShortcutsWhenDisabled": false
              }
            }
        """.trimIndent()

        prefsManager.restoreSettingsBackupJson(oldBackupJson)

        assertFalse(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
        assertFalse(prefsManager.areWifiNetworkRulesEnabled())
    }

    @Test
    fun wifiNetworkRules_restoreBackupWithOnlyInvalidRules_doesNotInventFallbackRules() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Existing", null, "default", null))
        val backup = Gson().fromJson(
            prefsManager.exportSettingsBackupJson(),
            JsonObject::class.java
        )
        val dns = backup.getAsJsonObject("dns")
        dns.remove("knownWifiNetworks")
        dns.addProperty("wifiNetworkRulesEnabled", true)
        dns.add("wifiNetworkRules", JsonArray().apply {
            add(JsonObject().apply {
                addProperty("id", "invalid-rule")
                addProperty("ssid", "Campus")
                addProperty("bssid", "not-a-bssid")
                addProperty("actionMode", DNS_MODE_AUTO)
            })
        })

        prefsManager.restoreSettingsBackupJson(Gson().toJson(backup))

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
    }

    @Test
    fun wifiNetworkRules_restoreBackupWithMissingRules_usesEmptyDefault() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Existing", null, DNS_MODE_OFF, null))
        val backup = Gson().fromJson(
            prefsManager.exportSettingsBackupJson(),
            JsonObject::class.java
        )
        val dns = backup.getAsJsonObject("dns")
        dns.remove("wifiNetworkRules")
        dns.remove("knownWifiNetworks")

        prefsManager.restoreSettingsBackupJson(Gson().toJson(backup))

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
    }

    @Test
    fun addCustomDnsHostname_whenCalled_thenCustomShortcutRemainsDisabledByDefault() {
        prefsManager.addCustomDnsHostname("New DNS", "new.example.com")

        val created =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "New DNS" }
        val shortcutId = "dns_custom_${created.id}"

        assertFalse(prefsManager.shortcutStore.enabledShortcutIds.value.contains(shortcutId))
    }

    @Test
    fun setAllowPinnedShortcutsWhenDisabled_whenCalled_thenUpdatesFlow() {
        prefsManager.shortcutStore.setAllowPinnedShortcutsWhenDisabled(true)

        assertTrue(prefsManager.shortcutStore.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun backupExport_whenCalled_thenIncludesReadableTimestamp() {
        val backup = Gson().fromJson(
            prefsManager.exportSettingsBackupJson(),
            SettingsBackup::class.java
        )

        assertTrue(backup.exportedAtEpochMillis > 0)
        assertEquals(
            Instant.ofEpochMilli(backup.exportedAtEpochMillis),
            Instant.parse(backup.exportedAtIso8601)
        )
    }

    @Test
    fun backupExportAndRestore_whenSettingsChanged_thenRestoresAllBackedUpValues() {
        prefsManager.setDnsToggleOff(false)
        prefsManager.setDnsToggleAuto(true)
        prefsManager.addCustomDnsHostname("Backup DNS", "backup.example.com")
        val customEntry =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "Backup DNS" }
        prefsManager.dnsStore.setSelected(customEntry.id, false)
        prefsManager.setDnsEnableAutoRevert(true)
        prefsManager.setDnsAutoRevertDelaySeconds(42)
        prefsManager.setDnsRequireUnlock(true)
        automation.setVpnDetectionEnabled(true)
        automation.setVpnDetectionMode(TILE_ONLY_DETECTION)
        automation.setNetworkTypeDetectionEnabled(true)
        automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        automation.setDnsStateOnWifi(DNS_MODE_ON)
        automation.setDnsHostnameOnWifi(customEntry.hostname)

        prefsManager.setUsbToggleEnable(false)
        prefsManager.setUsbToggleDisable(true)
        prefsManager.setUsbAlsoHideDevOptions(true)
        prefsManager.setUsbAlsoDisableWirelessDebugging(true)
        prefsManager.setUsbEnableAutoRevert(true)
        prefsManager.setUsbAutoRevertDelaySeconds(33)
        prefsManager.setUsbRequireUnlock(true)

        val customShortcutId = "dns_custom_${customEntry.id}"
        assertTrue(prefsManager.shortcutStore.setShortcutExposureEnabled(customShortcutId, true))
        assertTrue(prefsManager.shortcutStore.setShortcutFavorite(customShortcutId, true))
        prefsManager.shortcutStore.setAllowPinnedShortcutsWhenDisabled(true)

        val backupJson = prefsManager.exportSettingsBackupJson()

        prefsManager.setDnsToggleOff(true)
        prefsManager.deleteCustomDnsHostname(customEntry.id)
        prefsManager.setUsbToggleEnable(true)
        prefsManager.shortcutStore.setAllowPinnedShortcutsWhenDisabled(false)

        prefsManager.restoreSettingsBackupJson(backupJson)

        val restoredCustomEntry =
            prefsManager.dnsHostnames.value.firstOrNull { it.id == customEntry.id }
        assertNotNull(restoredCustomEntry)
        assertEquals("Backup DNS", restoredCustomEntry?.name)
        assertEquals("backup.example.com", restoredCustomEntry?.hostname)
        assertFalse(restoredCustomEntry?.isSelectedForCycle ?: true)
        assertFalse(prefsManager.dnsToggleOff.value)
        assertTrue(prefsManager.dnsEnableAutoRevert.value)
        assertEquals(42, prefsManager.dnsAutoRevertDelaySeconds.value)
        assertTrue(prefsManager.dnsRequireUnlock.value)
        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertEquals(DNS_MODE_ON, automation.dnsStateOnWifi.value)
        assertEquals("backup.example.com", automation.dnsHostnameOnWifi.value)

        assertFalse(prefsManager.usbStore.state.value.toggleEnable)
        assertTrue(prefsManager.usbStore.state.value.alsoHideDevOptions)
        assertTrue(prefsManager.usbStore.state.value.alsoDisableWirelessDebugging)
        assertTrue(prefsManager.usbStore.state.value.enableAutoRevert)
        assertEquals(33, prefsManager.usbStore.state.value.autoRevertDelaySeconds)
        assertTrue(prefsManager.usbStore.state.value.requireUnlock)

        assertTrue(prefsManager.shortcutStore.enabledShortcutIds.value.contains(customShortcutId))
        assertTrue(prefsManager.shortcutStore.favoriteShortcutIds.value.contains(customShortcutId))
        assertTrue(prefsManager.shortcutStore.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun backupRestore_whenValuesAreInvalid_thenNormalizesBeforeApplying() {
        val malformedBackupJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1,
              "dns": {
                "toggleOff": true,
                "toggleAuto": true,
                "hostnames": [
                  {
                    "id": "bad",
                    "name": "Bad DNS",
                    "hostname": "-invalid.example.com",
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  },
                  {
                    "id": "good",
                    "name": " Good DNS ",
                    "hostname": "Good.Example.COM",
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  }
                ],
                "enableAutoRevert": true,
                "autoRevertDelaySeconds": -5,
                "requireUnlock": true,
                "vpnDetectionEnabled": true,
                "vpnDetectionMode": "bad_mode",
                "networkTypeDetectionEnabled": true,
                "networkTypeDetectionMode": "also_bad",
                "dnsStateOnWifi": "invalid_dns_state",
                "dnsHostnameOnWifi": "Good.Example.COM",
                "dnsStateOnMobile": "hostname",
                "dnsHostnameOnMobile": "Good.Example.COM"
              },
              "usb": {
                "toggleEnable": true,
                "toggleDisable": true,
                "alsoHideDevOptions": false,
                "alsoDisableWirelessDebugging": false,
                "enableAutoRevert": true,
                "autoRevertDelaySeconds": 9999999,
                "requireUnlock": false
              },
              "shortcuts": {
                "enabledShortcutIds": ["dns_custom_good", "unknown_future_shortcut"],
                "favoriteShortcutIds": ["dns_custom_good"],
                "allowPinnedShortcutsWhenDisabled": true
              },
              "features": {
                "future.networkRules": {
                  "schemaVersion": 1
                }
              }
            }
        """.trimIndent()

        prefsManager.restoreSettingsBackupJson(malformedBackupJson)

        assertNull(prefsManager.dnsHostnames.value.firstOrNull { it.id == "bad" })
        val restoredGood = prefsManager.dnsHostnames.value.firstOrNull { it.id == "good" }
        assertNotNull(restoredGood)
        assertEquals("Good DNS", restoredGood?.name)
        assertEquals("good.example.com", restoredGood?.hostname)

        assertEquals(1, prefsManager.dnsAutoRevertDelaySeconds.value)
        assertEquals(86_400, prefsManager.usbStore.state.value.autoRevertDelaySeconds)
        assertEquals(TILE_ONLY_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(TILE_ONLY_DETECTION, automation.networkTypeDetectionMode.value)
        assertEquals(DNS_MODE_OFF, automation.dnsStateOnWifi.value)
        assertNull(automation.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_ON, automation.dnsStateOnMobile.value)
        assertEquals("good.example.com", automation.dnsHostnameOnMobile.value)
        assertTrue(prefsManager.shortcutStore.enabledShortcutIds.value.contains("dns_custom_good"))
        assertFalse(prefsManager.shortcutStore.enabledShortcutIds.value.contains("unknown_future_shortcut"))
    }

    @Test
    fun backupRestore_whenGsonNullsNonNullableFields_thenSkipsInvalidEntries() {
        val backupWithNullFieldsJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1,
              "dns": {
                "toggleOff": true,
                "toggleAuto": true,
                "hostnames": [
                  {
                    "id": "null_name",
                    "name": null,
                    "hostname": "null-name.example.com",
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  },
                  {
                    "id": null,
                    "name": "Null ID",
                    "hostname": "null-id.example.com",
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  },
                  {
                    "id": "null_hostname",
                    "name": "Null Hostname",
                    "hostname": null,
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  }
                ],
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false,
                "vpnDetectionEnabled": false,
                "vpnDetectionMode": "tile_only",
                "networkTypeDetectionEnabled": false,
                "networkTypeDetectionMode": "tile_only",
                "dnsStateOnWifi": "off",
                "dnsHostnameOnWifi": null,
                "dnsStateOnMobile": "opportunistic",
                "dnsHostnameOnMobile": null
              },
              "usb": {
                "toggleEnable": true,
                "toggleDisable": true,
                "alsoHideDevOptions": false,
                "alsoDisableWirelessDebugging": false,
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false
              },
              "shortcuts": {
                "enabledShortcutIds": [null, "dns_off"],
                "favoriteShortcutIds": [null, "dns_off"],
                "allowPinnedShortcutsWhenDisabled": false
              }
            }
        """.trimIndent()

        prefsManager.restoreSettingsBackupJson(backupWithNullFieldsJson)

        assertNull(prefsManager.dnsHostnames.value.firstOrNull { it.id == "null_name" })
        assertNull(prefsManager.dnsHostnames.value.firstOrNull { it.name == "Null ID" })
        assertNull(prefsManager.dnsHostnames.value.firstOrNull { it.id == "null_hostname" })
        assertEquals(setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF), prefsManager.shortcutStore.enabledShortcutIds.value)
        assertEquals(setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF), prefsManager.shortcutStore.favoriteShortcutIds.value)
    }

    @Test
    fun backupRestore_whenEnabledShortcutsExceedDeviceLimit_thenCapsAndPublishesOnlyRestoredIds() {
        val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager?
        val maxShortcutCount = prefsManager.shortcutMaxCount.value
        val customHostnames = (1..(maxShortcutCount + 3)).map { index ->
            """
                  {
                    "id": "custom_$index",
                    "name": "Custom $index",
                    "hostname": "custom$index.example.com",
                    "isPredefined": false,
                    "isSelectedForCycle": true
                  }
            """.trimIndent()
        }
        val enabledShortcutIds = listOf(
            ShortcutCatalog.SHORTCUT_ID_DNS_OFF,
            ShortcutCatalog.SHORTCUT_ID_DNS_AUTO,
            ShortcutCatalog.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutCatalog.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutCatalog.SHORTCUT_ID_DNS_QUAD9,
            ShortcutCatalog.SHORTCUT_ID_USB_ON,
            ShortcutCatalog.SHORTCUT_ID_USB_OFF
        ) + (1..(maxShortcutCount + 3)).map { "dns_custom_custom_$it" }
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1,
              "dns": {
                "toggleOff": true,
                "toggleAuto": true,
                "hostnames": [
            ${customHostnames.joinToString(",\n").prependIndent("      ")}
                ],
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false,
                "vpnDetectionEnabled": false,
                "vpnDetectionMode": "tile_only",
                "networkTypeDetectionEnabled": false,
                "networkTypeDetectionMode": "tile_only",
                "dnsStateOnWifi": "off",
                "dnsHostnameOnWifi": null,
                "dnsStateOnMobile": "opportunistic",
                "dnsHostnameOnMobile": null
              },
              "usb": {
                "toggleEnable": true,
                "toggleDisable": true,
                "alsoHideDevOptions": false,
                "alsoDisableWirelessDebugging": false,
                "enableAutoRevert": false,
                "autoRevertDelaySeconds": 5,
                "requireUnlock": false
              },
              "shortcuts": {
                "enabledShortcutIds": [
            ${enabledShortcutIds.joinToString(",\n") { "\"$it\"" }.prependIndent("      ")}
                ],
                "favoriteShortcutIds": [],
                "allowPinnedShortcutsWhenDisabled": false
              }
            }
        """.trimIndent()

        prefsManager.restoreSettingsBackupJson(backupJson)

        assertEquals(maxShortcutCount, prefsManager.shortcutStore.enabledShortcutIds.value.size)
        assertTrue(prefsManager.shortcutStore.enabledShortcutIds.value.all { shortcutId ->
            prefsManager.canExecuteShortcutId(shortcutId)
        })
        if (shortcutManager != null) {
            assertEquals(
                prefsManager.shortcutStore.enabledShortcutIds.value,
                shortcutManager.dynamicShortcuts.map { it.id }.toSet()
            )
            assertEquals(maxShortcutCount, shortcutManager.dynamicShortcuts.size)
        }
    }

    @Test
    fun backupRestore_whenRequiredCoreSectionsAreMissing_thenRejectsBackup() {
        val missingCoreSectionJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1,
              "features": {
                "future.tile": {
                  "schemaVersion": 1
                }
              }
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            prefsManager.restoreSettingsBackupJson(missingCoreSectionJson)
        }

        assertEquals(DNS_MODE_AUTO, automation.dnsStateOnMobile.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialog() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionResult(false)

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertTrue(automation.showNotificationPermissionFallbackDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialog() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertTrue(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun continueWithoutNotificationPermission_whenCalled_keepsBackgroundDetection() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        automation.onNotificationPermissionResult(false)

        automation.continueWithoutNotificationPermission()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertFalse(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialogWithoutFallback() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, prefsManager.getVpnDetectionMode())
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertTrue(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun openNotificationPermissionSettings_whenCalled_thenDoesNotFallbackToTileOnly() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        automation.onNotificationPermissionPermanentlyDenied()

        automation.openNotificationPermissionSettings()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun setVpnDetectionMode_whenNotificationPermissionMissing_appliesImmediatelyWithoutPrompt() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(TILE_ONLY_DETECTION)

        automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
        assertFalse(automation.notificationPermissionExplanationFromBackup.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialogWithoutStatusToast() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionResult(granted = false)

        assertEquals(BACKGROUND_DETECTION, prefsManager.getVpnDetectionMode())
        assertTrue(automation.showNotificationPermissionFallbackDialog.value)
        assertNull(automation.permissionGrantStatus.value)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    @Config(sdk = [33])
    fun backupRestore_reconcilesMonitoringAndSettingsReturnWithoutRepeatingExplanation() {
        shadowOf(context.applicationContext as Application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        shadowOf(context.applicationContext as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        val backup = prefsManager.exportSettingsBackupJson()
        prefsManager.setNetworkTypeDetectionEnabled(false)
        prefsManager.restoreSettingsBackupJson(backup)
        shadowOf(context.applicationContext as Application).clearStartedServices()
        automation.onBackupRestored(context)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(com.rbn.qtsettings.services.NetworkMonitoringService::class.java.name,
            shadowOf(context.applicationContext as Application).nextStartedService.component?.className)
        assertTrue(automation.showNotificationPermissionExplanationDialog.value)
        automation.requestNotificationPermissionFromExplanation()
        automation.onNotificationPermissionPermanentlyDenied()
        automation.openNotificationPermissionSettings()
        shadowOf(context.applicationContext as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        automation.refreshNotificationPermissionAfterSettings(context)
        automation.refreshNotificationPermissionAfterSettings(context)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
        assertFalse(automation.showNotificationPermissionSettingsDialog.value)
        assertEquals(0, automation.requestNotificationPermission.value)
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = com.rbn.qtsettings.app.AppStores::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

}
