package com.rbn.qtsettings

import android.util.Log
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.app.AppStores
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class AutomationFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun networkTypeDetection_shouldShowInDnsSettings() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing Network Type Detection visibility in DNS settings")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Verify Network Type Detection info button is present
        composeTestRule.onNodeWithText(
            context.getString(R.string.automation_network_details)
        ).performScrollTo()
            .assertIsDisplayed()

        // Note: Description text may not be in semantic tree, so we just verify main elements exist
        Log.i(FLOW_TAG, "Network Type Detection section displayed successfully")
    }

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
    fun networkTypeDetection_detectionModeShouldBeSelectable() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing Network Type Detection mode selection")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        ensureNetworkTypeDetectionEnabled()

        // Scroll to detection mode section
        composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().performClick()
        // Verify tile-only mode is visible
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TILE_OPTION)
            .performScrollTo()
            .assertIsDisplayed()

        // Verify background mode is visible
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
            .performScrollTo()
            .assertIsDisplayed()

        // Test clicking background mode (requires notification permission on Android 13+)
        if (canUseBackgroundDetection) {
            Log.i(FLOW_TAG, "Testing background mode selection")
            composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().performClick()
        } else {
            Log.w(
                FLOW_TAG,
                "Skipping background mode selection; required permissions unavailable"
            )
        }

        // Test clicking tile-only mode
        Log.i(FLOW_TAG, "Testing tile-only mode selection")
        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TILE_OPTION)
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(FLOW_TAG, "Detection mode selection works correctly")
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

    @Test
    fun networkTypeDetection_shouldCoexistWithVpnDetection() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing Network Type Detection and VPN Detection coexistence")

        // Verify VPN Detection section is visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_vpn_detection_enabled))

        // Read the actual switch state; mode rows remain visible while disabled.
        val vpnAlreadyEnabled = composeTestRule.onNodeWithTag("vpn_detection_toggle")
            .fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On

        if (!vpnAlreadyEnabled) {
            Log.i(FLOW_TAG, "Enabling VPN Detection")
            composeTestRule.onNodeWithText(context.getString(R.string.setting_vpn_detection_enabled))
                .performClick()
            composeTestRule.waitForIdle()
        } else {
            Log.i(FLOW_TAG, "VPN Detection already enabled")
        }

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        ensureNetworkTypeDetectionEnabled()

        // Verify both sections are enabled and visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_vpn_detection_enabled))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Verify Network Type Detection options are visible when enabled
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.automation_wifi))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.automation_mobile))

        Log.i(FLOW_TAG, "Network Type Detection and VPN Detection coexist correctly")
    }

    @Test
    fun networkTypeDetection_shouldShowAllDnsStateOptions() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsAutomation(context)

        Log.i(FLOW_TAG, "Testing DNS state options in Network Type Detection")

        ensureNetworkTypeDetectionEnabled()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
        composeTestRule.navigateToDnsProviders(context)
        // Add a custom DNS hostname first to test hostname option
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_add_custom_hostname_button))
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_custom_hostname_button))
            .performClick()
        composeTestRule.waitForIdle()

        // Fill in custom hostname
        val testHostname = "Test Network Type DNS"
        val testValue = "dns.networktype.test"
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(testHostname)
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput(testValue)

        // Save the hostname
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify custom hostname is added
        composeTestRule.safeScrollToAndAssert(testHostname)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
        composeTestRule.navigateToDnsAutomation(context)
        // Scroll back to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.automation_wifi))

        composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL).performClick()
        composeTestRule.onNodeWithTag("automation_target_off").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("automation_target_auto").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(testHostname).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()

        // Verify Mobile DNS state label is displayed
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.automation_mobile))
        composeTestRule.onNodeWithText(context.getString(R.string.automation_mobile))
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
        composeTestRule.navigateToDnsProviders(context)
        // Clean up: delete the test hostname
        try {
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_entry_actions, testHostname, testValue)
            )
                .performScrollTo()
                .performClick()
            composeTestRule.onNodeWithTag("dns_delete_button_${testValue}_${testHostname}")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(context.getString(R.string.button_delete))
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // If cleanup fails, log it but don't fail the test
            Log.w(FLOW_TAG, "Failed to clean up test hostname: ${e.message}")
        }

        Log.i(FLOW_TAG, "DNS state options in Network Type Detection work correctly")
    }
}
