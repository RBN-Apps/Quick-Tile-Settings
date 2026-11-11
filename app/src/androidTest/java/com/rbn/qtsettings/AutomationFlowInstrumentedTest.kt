package com.rbn.qtsettings

import android.util.Log
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomationFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun networkTypeDetection_shouldHandleEnableDisableToggle() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing Network Type Detection enable/disable toggle")

        // Scroll to Network Type Detection checkbox
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        val networkTypeInitiallyEnabled = networkDetectionIsEnabled()

        if (networkTypeInitiallyEnabled) {
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Click to enable Network Type Detection
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL).assertIsEnabled()
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL).assertIsEnabled()
        // Verify WiFi DNS state selector appears when enabled
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL)
            .performScrollTo()
            .assertIsDisplayed()

        // Verify Mobile DNS state selector appears when enabled
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL)
            .performScrollTo()
            .assertIsDisplayed()

        // Click to disable Network Type Detection
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL).assertIsNotEnabled()
        Log.i(FLOW_TAG, "Network Type Detection toggle works correctly")
    }

    @Test
    fun networkTypeDetection_dnsStateSelectorsShouldWork() {
        composeTestRule.navigateToDnsAutomation(context)
        ensureNetworkTypeDetectionEnabled()
        val preferences = com.rbn.qtsettings.app.AppStores.getInstance(context)
        val oldWifi = preferences.automation.getDnsStateOnWifi()
        val oldWifiHost = preferences.automation.getDnsHostnameOnWifi()
        val oldMobile = preferences.automation.getDnsStateOnMobile()
        val oldMobileHost = preferences.automation.getDnsHostnameOnMobile()
        try {
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL).performScrollTo().performClick()
            composeTestRule.onNodeWithTag("automation_target_auto").performScrollTo().performClick()
            composeTestRule.runOnIdle {
                org.junit.Assert.assertEquals(com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO, preferences.automation.getDnsStateOnWifi())
                org.junit.Assert.assertNull(preferences.automation.getDnsHostnameOnWifi())
            }
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL).performScrollTo().performClick()
            composeTestRule.onNodeWithTag("automation_target_off").performScrollTo().performClick()
            composeTestRule.runOnIdle {
                org.junit.Assert.assertEquals(com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF, preferences.automation.getDnsStateOnMobile())
                org.junit.Assert.assertNull(preferences.automation.getDnsHostnameOnMobile())
            }
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL).performScrollTo().performClick()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
            org.junit.Assert.assertEquals(com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO, preferences.automation.getDnsStateOnWifi())
        } finally {
            preferences.automation.setDnsStateOnWifi(oldWifi); preferences.automation.setDnsHostnameOnWifi(oldWifiHost)
            preferences.automation.setDnsStateOnMobile(oldMobile); preferences.automation.setDnsHostnameOnMobile(oldMobileHost)
        }
    }

    @Test
    fun networkTypeDetection_detectionModeSelectionIsStored() {
        val automation = AppStores.getInstance(context).automation
        val originalEnabled = automation.isNetworkTypeDetectionEnabled()
        val originalMode = automation.getNetworkTypeDetectionMode()
        try {
            // Start from background so choosing tile-only is an observable change.
            automation.setNetworkTypeDetectionMode(Constants.BACKGROUND_DETECTION)
            composeTestRule.navigateToDnsAutomation(context)
            ensureNetworkTypeDetectionEnabled()

            composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().performClick()
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
                .performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TILE_OPTION)
                .performScrollTo().performClick()
            composeTestRule.runOnIdle {
                assertEquals(Constants.TILE_ONLY_DETECTION, automation.getNetworkTypeDetectionMode())
            }
        } finally {
            automation.setNetworkTypeDetectionMode(originalMode)
            automation.setNetworkTypeDetectionEnabled(originalEnabled)
        }
    }

    @Test
    fun networkTypeInfoDialog_shouldDisplayAndDismiss() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing Network Type Detection info dialog")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Click the info button
        composeTestRule.onNodeWithText(
            context.getString(R.string.automation_network_details)
        ).performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Wait for info dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.network_type_info_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify dialog title is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_title))
            .assertIsDisplayed()

        // Verify dialog message is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_message))
            .assertIsDisplayed()

        // Close the dialog
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_title))
            .assertDoesNotExist()

        Log.i(FLOW_TAG, "Network Type Detection info dialog displayed and dismissed successfully")
    }

    /** Enabling one detection must not switch the other one off. */
    @Test
    fun networkTypeDetection_andVpnDetectionCanBeEnabledTogether() {
        val automation = AppStores.getInstance(context).automation
        val originalVpn = automation.isVpnDetectionEnabled()
        val originalNetwork = automation.isNetworkTypeDetectionEnabled()
        try {
            composeTestRule.navigateToDnsAutomation(context)
            composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_vpn_detection_enabled))
            val vpnToggle = composeTestRule.onNodeWithTag("vpn_detection_toggle")
            if (vpnToggle.fetchSemanticsNode().config[SemanticsProperties.ToggleableState] != ToggleableState.On) {
                vpnToggle.performScrollTo().performClick()
            }
            ensureNetworkTypeDetectionEnabled()

            composeTestRule.runOnIdle {
                assertTrue(automation.isVpnDetectionEnabled())
                assertTrue(automation.isNetworkTypeDetectionEnabled())
            }
            composeTestRule.onNodeWithTag("vpn_detection_toggle").assertIsOn()
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE).assertIsOn()
        } finally {
            automation.setVpnDetectionEnabled(originalVpn)
            automation.setNetworkTypeDetectionEnabled(originalNetwork)
        }
    }

    /** Custom providers from the DNS feature must be offered as automation targets. */
    @Test
    fun networkTypeDetection_offersSystemModesAndCustomProvidersAsTargets() {
        val stores = AppStores.getInstance(context)
        val originalNetwork = stores.automation.isNetworkTypeDetectionEnabled()
        val name = "Test Network Type DNS"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            stores.dns.addCustomDnsHostname(name, "dns.networktype.test")
        }
        try {
            composeTestRule.navigateToDnsAutomation(context)
            ensureNetworkTypeDetectionEnabled()
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL)
                .performScrollTo().performClick()
            composeTestRule.onNodeWithTag("automation_target_off").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("automation_target_auto").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                stores.dns.dnsHostnames.value.filter { it.name == name }
                    .forEach { stores.dns.deleteCustomDnsHostname(it.id) }
            }
            stores.automation.setNetworkTypeDetectionEnabled(originalNetwork)
        }
    }
}
