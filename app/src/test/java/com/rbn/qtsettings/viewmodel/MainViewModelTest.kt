package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.ShortcutManager
import com.google.gson.Gson
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.SettingsBackup
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.ShortcutUtils
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
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        prefsManager = PreferencesManager.getInstance(context)
        viewModel = MainViewModel(prefsManager)
        viewModel.setApplicationContext(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun setInitialTab_whenCalled_thenUpdatesInitialTabFlow() {
        viewModel.setInitialTab(1)
        assertEquals(1, viewModel.initialTab.value)
    }

    @Test
    fun startAddingNewHostname_whenCalled_thenShowsDialogAndClearsEditingEntry() {
        val existing = DnsHostnameEntry(id = "edit", name = "Edit Me", hostname = "dns.edit")
        viewModel.startEditingHostname(existing)

        viewModel.startAddingNewHostname()

        assertTrue(viewModel.showHostnameEditDialog.value)
        assertNull(viewModel.editingHostname.value)
    }

    @Test
    fun startEditingAndDismissHostname_whenCalled_thenDialogStateIsManagedCorrectly() {
        val entry = DnsHostnameEntry(id = "host-1", name = "Host", hostname = "dns.host")

        viewModel.startEditingHostname(entry)
        assertTrue(viewModel.showHostnameEditDialog.value)
        assertEquals(entry, viewModel.editingHostname.value)

        viewModel.dismissHostnameEditDialog()
        assertFalse(viewModel.showHostnameEditDialog.value)
        assertNull(viewModel.editingHostname.value)
    }

    @Test
    fun setHostnamePendingDeletion_whenCalled_thenFlowIsUpdated() {
        val entry = DnsHostnameEntry(id = "delete", name = "Delete", hostname = "dns.delete")

        viewModel.setHostnamePendingDeletion(entry)

        assertEquals(entry, viewModel.hostnamePendingDeletion.value)
    }

    @Test
    fun shortcutSettings_whenFreshlyInitialized_thenNoShortcutsEnabledAndPinnedFallbackDisabled() {
        assertTrue(viewModel.enabledShortcutIds.value.isEmpty())
        assertTrue(viewModel.favoriteShortcutIds.value.isEmpty())
        assertFalse(viewModel.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun updateDnsHostnameEntrySelection_whenEntryExists_thenSelectionIsUpdated() {
        val targetId = "adguard_default"
        val before = prefsManager.dnsHostnames.value.first { it.id == targetId }
        assertTrue(before.isSelectedForCycle)

        viewModel.updateDnsHostnameEntrySelection(targetId, false)

        val after = prefsManager.dnsHostnames.value.first { it.id == targetId }
        assertFalse(after.isSelectedForCycle)
    }

    @Test
    fun editCustomDnsHostname_whenCustomEntryExists_thenNameAndHostnameAreUpdated() {
        viewModel.addCustomDnsHostname("Old Name", "old.example.com")
        val created =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "Old Name" }

        viewModel.editCustomDnsHostname(created.id, "New Name", "new.example.com")

        val updated = prefsManager.dnsHostnames.value.firstOrNull { it.id == created.id }
        assertNotNull(updated)
        assertEquals("New Name", updated?.name)
        assertEquals("new.example.com", updated?.hostname)
    }

    @Test
    fun addCustomDnsHostname_whenCalled_thenCustomShortcutRemainsDisabledByDefault() {
        viewModel.addCustomDnsHostname("New DNS", "new.example.com")

        val created =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "New DNS" }
        val shortcutId = "dns_custom_${created.id}"

        assertFalse(viewModel.enabledShortcutIds.value.contains(shortcutId))
    }

    @Test
    fun setAllowPinnedShortcutsWhenDisabled_whenCalled_thenUpdatesFlow() {
        viewModel.setAllowPinnedShortcutsWhenDisabled(true)

        assertTrue(viewModel.allowPinnedShortcutsWhenDisabled.value)
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
        viewModel.setDnsToggleOff(false)
        viewModel.setDnsToggleAuto(true)
        viewModel.addCustomDnsHostname("Backup DNS", "backup.example.com")
        val customEntry =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "Backup DNS" }
        viewModel.updateDnsHostnameEntrySelection(customEntry.id, false)
        viewModel.setDnsEnableAutoRevert(true)
        viewModel.setDnsAutoRevertDelaySeconds(42)
        viewModel.setDnsRequireUnlock(true)
        viewModel.setVpnDetectionEnabled(true)
        viewModel.setVpnDetectionMode(TILE_ONLY_DETECTION)
        viewModel.setNetworkTypeDetectionEnabled(true)
        viewModel.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        viewModel.setDnsStateOnWifi(DNS_MODE_ON)
        viewModel.setDnsHostnameOnWifi(customEntry.hostname)

        viewModel.setUsbToggleEnable(false)
        viewModel.setUsbToggleDisable(true)
        viewModel.setUsbAlsoHideDevOptions(true)
        viewModel.setUsbAlsoDisableWirelessDebugging(true)
        viewModel.setUsbEnableAutoRevert(true)
        viewModel.setUsbAutoRevertDelaySeconds(33)
        viewModel.setUsbRequireUnlock(true)

        val customShortcutId = "dns_custom_${customEntry.id}"
        assertTrue(viewModel.setShortcutExposureEnabled(customShortcutId, true))
        assertTrue(viewModel.setShortcutFavorite(customShortcutId, true))
        viewModel.setAllowPinnedShortcutsWhenDisabled(true)

        val backupJson = prefsManager.exportSettingsBackupJson()

        viewModel.setDnsToggleOff(true)
        viewModel.deleteCustomDnsHostname(customEntry.id)
        viewModel.setUsbToggleEnable(true)
        viewModel.setAllowPinnedShortcutsWhenDisabled(false)

        prefsManager.restoreSettingsBackupJson(backupJson)

        val restoredCustomEntry =
            prefsManager.dnsHostnames.value.firstOrNull { it.id == customEntry.id }
        assertNotNull(restoredCustomEntry)
        assertEquals("Backup DNS", restoredCustomEntry?.name)
        assertEquals("backup.example.com", restoredCustomEntry?.hostname)
        assertFalse(restoredCustomEntry?.isSelectedForCycle ?: true)
        assertFalse(viewModel.dnsToggleOff.value)
        assertTrue(viewModel.dnsEnableAutoRevert.value)
        assertEquals(42, viewModel.dnsAutoRevertDelaySeconds.value)
        assertTrue(viewModel.dnsRequireUnlock.value)
        assertTrue(viewModel.networkTypeDetectionEnabled.value)
        assertEquals(DNS_MODE_ON, viewModel.dnsStateOnWifi.value)
        assertEquals("backup.example.com", viewModel.dnsHostnameOnWifi.value)

        assertFalse(viewModel.usbToggleEnable.value)
        assertTrue(viewModel.usbAlsoHideDevOptions.value)
        assertTrue(viewModel.usbAlsoDisableWirelessDebugging.value)
        assertTrue(viewModel.usbEnableAutoRevert.value)
        assertEquals(33, viewModel.usbAutoRevertDelaySeconds.value)
        assertTrue(viewModel.usbRequireUnlock.value)

        assertTrue(viewModel.enabledShortcutIds.value.contains(customShortcutId))
        assertTrue(viewModel.favoriteShortcutIds.value.contains(customShortcutId))
        assertTrue(viewModel.allowPinnedShortcutsWhenDisabled.value)
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

        assertEquals(1, viewModel.dnsAutoRevertDelaySeconds.value)
        assertEquals(86_400, viewModel.usbAutoRevertDelaySeconds.value)
        assertEquals(TILE_ONLY_DETECTION, viewModel.vpnDetectionMode.value)
        assertEquals(TILE_ONLY_DETECTION, viewModel.networkTypeDetectionMode.value)
        assertEquals(DNS_MODE_OFF, viewModel.dnsStateOnWifi.value)
        assertNull(viewModel.dnsHostnameOnWifi.value)
        assertEquals(DNS_MODE_ON, viewModel.dnsStateOnMobile.value)
        assertEquals("good.example.com", viewModel.dnsHostnameOnMobile.value)
        assertTrue(viewModel.enabledShortcutIds.value.contains("dns_custom_good"))
        assertFalse(viewModel.enabledShortcutIds.value.contains("unknown_future_shortcut"))
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
        assertEquals(setOf(ShortcutUtils.SHORTCUT_ID_DNS_OFF), viewModel.enabledShortcutIds.value)
        assertEquals(setOf(ShortcutUtils.SHORTCUT_ID_DNS_OFF), viewModel.favoriteShortcutIds.value)
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
            ShortcutUtils.SHORTCUT_ID_DNS_OFF,
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
            ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
            ShortcutUtils.SHORTCUT_ID_USB_ON,
            ShortcutUtils.SHORTCUT_ID_USB_OFF
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

        assertEquals(maxShortcutCount, viewModel.enabledShortcutIds.value.size)
        assertTrue(viewModel.enabledShortcutIds.value.all { shortcutId ->
            prefsManager.canExecuteShortcutId(shortcutId)
        })
        if (shortcutManager != null) {
            assertEquals(
                viewModel.enabledShortcutIds.value,
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

        assertEquals(DNS_MODE_AUTO, viewModel.dnsStateOnMobile.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialog() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionResult(false)

        assertEquals(BACKGROUND_DETECTION, viewModel.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, viewModel.networkTypeDetectionMode.value)
        assertTrue(viewModel.showNotificationPermissionFallbackDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialog() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, viewModel.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, viewModel.networkTypeDetectionMode.value)
        assertFalse(viewModel.showNotificationPermissionFallbackDialog.value)
        assertTrue(viewModel.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun useTileOnlyDetectionForNotificationFallback_whenCalled_thenFallsBackBackgroundDetection() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        viewModel.onNotificationPermissionResult(false)

        viewModel.useTileOnlyDetectionForNotificationFallback()

        assertEquals(TILE_ONLY_DETECTION, viewModel.vpnDetectionMode.value)
        assertEquals(TILE_ONLY_DETECTION, viewModel.networkTypeDetectionMode.value)
        assertFalse(viewModel.showNotificationPermissionFallbackDialog.value)
        assertFalse(viewModel.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialogWithoutFallback() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, prefsManager.getVpnDetectionMode())
        assertFalse(viewModel.showNotificationPermissionFallbackDialog.value)
        assertTrue(viewModel.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun openNotificationPermissionSettings_whenCalled_thenDoesNotFallbackToTileOnly() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.setNetworkTypeDetectionEnabled(true)
        prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        viewModel.onNotificationPermissionPermanentlyDenied()

        viewModel.openNotificationPermissionSettings()

        assertEquals(BACKGROUND_DETECTION, viewModel.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, viewModel.networkTypeDetectionMode.value)
        assertFalse(viewModel.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun setVpnDetectionMode_whenNotificationPermissionMissing_thenShowsManualExplanationAndAppliesAfterGrant() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(TILE_ONLY_DETECTION)

        viewModel.setVpnDetectionMode(BACKGROUND_DETECTION)

        assertEquals(TILE_ONLY_DETECTION, viewModel.vpnDetectionMode.value)
        assertTrue(viewModel.showNotificationPermissionExplanationDialog.value)
        assertFalse(viewModel.notificationPermissionExplanationFromBackup.value)

        viewModel.onNotificationPermissionResult(granted = true)

        assertEquals(BACKGROUND_DETECTION, viewModel.vpnDetectionMode.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialogWithoutStatusToast() {
        prefsManager.setVpnDetectionEnabled(true)
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionResult(granted = false)

        assertEquals(BACKGROUND_DETECTION, prefsManager.getVpnDetectionMode())
        assertTrue(viewModel.showNotificationPermissionFallbackDialog.value)
        assertNull(viewModel.permissionGrantStatus.value)
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

}
