package com.rbn.qtsettings.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rbn.qtsettings.data.SettingsBackup
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsBackupIntegrationTest : SettingsIntegrationFixture() {

    @Test
    fun wifiNetworkRules_backupExportAndRestore_roundTripsEnabledStateAndRules() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Home", null, "default", null))
        assertTrue(automation.addWifiNetworkRule("Büro", null, "default", null))
        val backupJson = prefsManager.backup.exportSettingsBackupJson()
        val exportedBackup = Gson().fromJson(backupJson, SettingsBackup::class.java)

        assertTrue(exportedBackup.dns?.wifiNetworkRulesEnabled == true)

        automation.setWifiNetworkRulesEnabled(false)
        automation.wifiNetworkRules.value.forEach { automation.deleteWifiNetworkRule(it.id) }
        assertTrue(automation.addWifiNetworkRule("Temporary", null, DNS_MODE_OFF, null))

        prefsManager.backup.restoreSettingsBackupJson(backupJson)

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(
            setOf("Home", "Büro"),
            automation.wifiNetworkRules.value.mapNotNull { it.ssid }.toSet()
        )
        assertTrue(prefsManager.automation.areWifiNetworkRulesEnabled())
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
        prefsManager.automation.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
            seenAtEpochMillis = 1234
        )
        val backupJson = prefsManager.backup.exportSettingsBackupJson()

        automation.deleteWifiNetworkRule(automation.wifiNetworkRules.value.single().id)
        prefsManager.backup.restoreSettingsBackupJson(backupJson)

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

        prefsManager.backup.restoreSettingsBackupJson(oldBackupJson)

        assertFalse(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
        assertFalse(prefsManager.automation.areWifiNetworkRulesEnabled())
    }

    @Test
    fun wifiNetworkRules_restoreBackupWithOnlyInvalidRules_doesNotInventFallbackRules() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Existing", null, "default", null))
        val backup = Gson().fromJson(
            prefsManager.backup.exportSettingsBackupJson(),
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

        prefsManager.backup.restoreSettingsBackupJson(Gson().toJson(backup))

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
    }

    @Test
    fun wifiNetworkRules_restoreBackupWithMissingRules_usesEmptyDefault() {
        automation.setWifiNetworkRulesEnabled(true)
        assertTrue(automation.addWifiNetworkRule("Existing", null, DNS_MODE_OFF, null))
        val backup = Gson().fromJson(
            prefsManager.backup.exportSettingsBackupJson(),
            JsonObject::class.java
        )
        val dns = backup.getAsJsonObject("dns")
        dns.remove("wifiNetworkRules")
        dns.remove("knownWifiNetworks")

        prefsManager.backup.restoreSettingsBackupJson(Gson().toJson(backup))

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertTrue(automation.wifiNetworkRules.value.isEmpty())
    }

    @Test
    fun backupExport_whenCalled_thenIncludesReadableTimestamp() {
        val backup = Gson().fromJson(
            prefsManager.backup.exportSettingsBackupJson(),
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
        prefsManager.dns.setDnsToggleOff(false)
        prefsManager.dns.setDnsToggleAuto(true)
        prefsManager.dns.addCustomDnsHostname("Backup DNS", "backup.example.com")
        val customEntry =
            prefsManager.dns.dnsHostnames.value.first { !it.isPredefined && it.name == "Backup DNS" }
        prefsManager.dns.setSelected(customEntry.id, false)
        prefsManager.dns.setDnsEnableAutoRevert(true)
        prefsManager.dns.setDnsAutoRevertDelaySeconds(42)
        prefsManager.dns.setDnsRequireUnlock(true)
        automation.setVpnDetectionEnabled(true)
        automation.setVpnDetectionMode(TILE_ONLY_DETECTION)
        automation.setNetworkTypeDetectionEnabled(true)
        automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        automation.setDnsStateOnWifi(DNS_MODE_ON)
        automation.setDnsHostnameOnWifi(customEntry.hostname)

        prefsManager.usb.setToggleEnable(false)
        prefsManager.usb.setToggleDisable(true)
        prefsManager.usb.setAlsoHideDevOptions(true)
        prefsManager.usb.setAlsoDisableWirelessDebugging(true)
        prefsManager.usb.setEnableAutoRevert(true)
        prefsManager.usb.setAutoRevertDelaySeconds(33)
        prefsManager.usb.setRequireUnlock(true)

        val customShortcutId = "dns_custom_${customEntry.id}"
        assertTrue(prefsManager.shortcuts.setShortcutExposureEnabled(customShortcutId, true))
        assertTrue(prefsManager.shortcuts.setShortcutFavorite(customShortcutId, true))
        prefsManager.shortcuts.setAllowPinnedShortcutsWhenDisabled(true)

        val backupJson = prefsManager.backup.exportSettingsBackupJson()

        prefsManager.dns.setDnsToggleOff(true)
        prefsManager.dns.deleteCustomDnsHostname(customEntry.id)
        prefsManager.usb.setToggleEnable(true)
        prefsManager.shortcuts.setAllowPinnedShortcutsWhenDisabled(false)

        prefsManager.backup.restoreSettingsBackupJson(backupJson)

        val restoredCustomEntry =
            prefsManager.dns.dnsHostnames.value.firstOrNull { it.id == customEntry.id }
        assertNotNull(restoredCustomEntry)
        assertEquals("Backup DNS", restoredCustomEntry?.name)
        assertEquals("backup.example.com", restoredCustomEntry?.hostname)
        assertFalse(restoredCustomEntry?.isSelectedForCycle ?: true)
        assertFalse(prefsManager.dns.dnsToggleOff.value)
        assertTrue(prefsManager.dns.dnsEnableAutoRevert.value)
        assertEquals(42, prefsManager.dns.dnsAutoRevertDelaySeconds.value)
        assertTrue(prefsManager.dns.dnsRequireUnlock.value)
        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertEquals(DNS_MODE_ON, automation.dnsStateOnWifi.value)
        assertEquals("backup.example.com", automation.dnsHostnameOnWifi.value)

        assertFalse(prefsManager.usb.state.value.toggleEnable)
        assertTrue(prefsManager.usb.state.value.alsoHideDevOptions)
        assertTrue(prefsManager.usb.state.value.alsoDisableWirelessDebugging)
        assertTrue(prefsManager.usb.state.value.enableAutoRevert)
        assertEquals(33, prefsManager.usb.state.value.autoRevertDelaySeconds)
        assertTrue(prefsManager.usb.state.value.requireUnlock)

        assertTrue(prefsManager.shortcuts.enabledShortcutIds.value.contains(customShortcutId))
        assertTrue(prefsManager.shortcuts.favoriteShortcutIds.value.contains(customShortcutId))
        assertTrue(prefsManager.shortcuts.allowPinnedShortcutsWhenDisabled.value)
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

        prefsManager.backup.restoreSettingsBackupJson(malformedBackupJson)

        assertNull(prefsManager.dns.dnsHostnames.value.firstOrNull { it.id == "bad" })
        val restoredGood = prefsManager.dns.dnsHostnames.value.firstOrNull { it.id == "good" }
        assertNotNull(restoredGood)
        assertEquals("Good DNS", restoredGood?.name)
        assertEquals("good.example.com", restoredGood?.hostname)

        assertEquals(1, prefsManager.dns.dnsAutoRevertDelaySeconds.value)
        assertEquals(86_400, prefsManager.usb.state.value.autoRevertDelaySeconds)
        assertEquals(TILE_ONLY_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(TILE_ONLY_DETECTION, automation.networkTypeDetectionMode.value)
        assertEquals(DNS_MODE_OFF, automation.dnsStateOnWifi.value)
        assertNull(automation.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_ON, automation.dnsStateOnMobile.value)
        assertEquals("good.example.com", automation.dnsHostnameOnMobile.value)
        assertTrue(prefsManager.shortcuts.enabledShortcutIds.value.contains("dns_custom_good"))
        assertFalse(prefsManager.shortcuts.enabledShortcutIds.value.contains("unknown_future_shortcut"))
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

        prefsManager.backup.restoreSettingsBackupJson(backupWithNullFieldsJson)

        assertNull(prefsManager.dns.dnsHostnames.value.firstOrNull { it.id == "null_name" })
        assertNull(prefsManager.dns.dnsHostnames.value.firstOrNull { it.name == "Null ID" })
        assertNull(prefsManager.dns.dnsHostnames.value.firstOrNull { it.id == "null_hostname" })
        assertEquals(setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF), prefsManager.shortcuts.enabledShortcutIds.value)
        assertEquals(setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF), prefsManager.shortcuts.favoriteShortcutIds.value)
    }

    @Test
    fun backupRestore_whenEnabledShortcutsExceedDeviceLimit_thenCapsAndPublishesOnlyRestoredIds() {
        val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager?
        val maxShortcutCount = prefsManager.shortcuts.shortcutMaxCount.value
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

        prefsManager.backup.restoreSettingsBackupJson(backupJson)

        assertEquals(maxShortcutCount, prefsManager.shortcuts.enabledShortcutIds.value.size)
        assertTrue(prefsManager.shortcuts.enabledShortcutIds.value.all { shortcutId ->
            prefsManager.shortcuts.canExecuteShortcutId(shortcutId)
        })
        if (shortcutManager != null) {
            assertEquals(
                prefsManager.shortcuts.enabledShortcutIds.value,
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
            prefsManager.backup.restoreSettingsBackupJson(missingCoreSectionJson)
        }

        assertEquals(DNS_MODE_AUTO, automation.dnsStateOnMobile.value)
    }

    @Test
    @Config(sdk = [33])
    fun backupRestore_reconcilesMonitoringAndSettingsReturnWithoutRepeatingExplanation() {
        shadowOf(context.applicationContext as Application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        shadowOf(context.applicationContext as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        val backup = prefsManager.backup.exportSettingsBackupJson()
        prefsManager.automation.setNetworkTypeDetectionEnabled(false)
        prefsManager.backup.restoreSettingsBackupJson(backup)
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
}
