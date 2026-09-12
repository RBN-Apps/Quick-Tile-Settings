package com.rbn.qtsettings.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AutomationSettingsIntegrationTest : SettingsIntegrationFixture() {

    @Test
    fun wifiNetworkRules_explicitRuleCrudPersistsAcrossAppStoresRecreation() {
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

        resetAppStoresSingleton()
        val restoredAppStores = AppStores.getInstance(context)
        val restoredRule = restoredAppStores.automation.getWifiNetworkRules().single()

        assertEquals("Campus WiFi", restoredRule.ssid)
        assertNull(restoredRule.bssid)
        assertEquals(DNS_MODE_AUTO, restoredRule.actionMode)
        assertNull(restoredRule.dnsHostname)
    }

    @Test
    fun wifiNetworkHistory_recordsMostRecentIdentityAndDeduplicatesIt() {
        prefsManager.automation.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "aa:bb:cc:dd:ee:ff"),
            seenAtEpochMillis = 100
        )
        prefsManager.automation.recordKnownWifiNetwork(
            WifiNetworkIdentity("Home", "11:22:33:44:55:66"),
            seenAtEpochMillis = 200
        )
        prefsManager.automation.recordKnownWifiNetwork(
            WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
            seenAtEpochMillis = 300
        )

        val history = prefsManager.automation.getKnownWifiNetworks()
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
        prefsManager.automation.setNetworkTypeDetectionEnabled(false)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        prefsManager.automation.setWifiNetworkRulesEnabled(true)

        automation.setNetworkTypeDetectionEnabled(true)

        assertTrue(automation.networkTypeDetectionEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
    }

    @Test
    @Config(sdk = [32])
    fun setWifiNetworkRulesEnabled_whenNetworkDetectionIsEnabled_forcesBackgroundAndShowsStatus() {
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

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
        val contextFreeViewModel = AutomationViewModel(prefsManager.automation, { com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource(it) })
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        prefsManager.automation.setWifiNetworkRulesEnabled(true)

        contextFreeViewModel.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

        assertEquals(BACKGROUND_DETECTION, contextFreeViewModel.networkTypeDetectionMode.value)
    }

    @Test
    @Config(sdk = [33])
    fun setWifiNetworkRulesEnabled_whenNotificationPermissionIsMissing_activatesImmediatelyWithoutPrompt() {
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

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
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
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
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)

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
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        automation.setWifiNetworkRulesEnabled(true)
        automation.continueWithoutNotificationPermission()

        assertTrue(automation.wifiNetworkRulesEnabled.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun continueWithoutNotifications_whenEnablingDetectionWithActiveRules_keepsRulesEnabled() {
        prefsManager.automation.setNetworkTypeDetectionEnabled(false)
        prefsManager.automation.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
        prefsManager.automation.setWifiNetworkRulesEnabled(true)

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
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialog() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionResult(false)

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertTrue(automation.showNotificationPermissionFallbackDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialog() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertTrue(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun continueWithoutNotificationPermission_whenCalled_keepsBackgroundDetection() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        automation.onNotificationPermissionResult(false)

        automation.continueWithoutNotificationPermission()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertFalse(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenShowsSettingsDialogWithoutFallback() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionPermanentlyDenied()

        assertEquals(BACKGROUND_DETECTION, prefsManager.automation.getVpnDetectionMode())
        assertFalse(automation.showNotificationPermissionFallbackDialog.value)
        assertTrue(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    fun openNotificationPermissionSettings_whenCalled_thenDoesNotFallbackToTileOnly() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)
        prefsManager.automation.setNetworkTypeDetectionEnabled(true)
        prefsManager.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        automation.onNotificationPermissionPermanentlyDenied()

        automation.openNotificationPermissionSettings()

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertEquals(BACKGROUND_DETECTION, automation.networkTypeDetectionMode.value)
        assertFalse(automation.showNotificationPermissionSettingsDialog.value)
    }

    @Test
    @Config(sdk = [33])
    fun setVpnDetectionMode_whenNotificationPermissionMissing_appliesImmediatelyWithoutPrompt() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(TILE_ONLY_DETECTION)

        automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        assertEquals(BACKGROUND_DETECTION, automation.vpnDetectionMode.value)
        assertFalse(automation.showNotificationPermissionExplanationDialog.value)
        assertFalse(automation.notificationPermissionExplanationFromBackup.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenShowsFallbackDialogWithoutStatusToast() {
        prefsManager.automation.setVpnDetectionEnabled(true)
        prefsManager.automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        automation.onNotificationPermissionResult(granted = false)

        assertEquals(BACKGROUND_DETECTION, prefsManager.automation.getVpnDetectionMode())
        assertTrue(automation.showNotificationPermissionFallbackDialog.value)
        assertNull(automation.permissionGrantStatus.value)
    }
}
