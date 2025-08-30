package com.rbn.qtsettings

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "QuickTileSettingsTest"

@RunWith(AndroidJUnit4::class)
class QuickTileSettingsInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.waitForIdle()

        composeTestRule.dismissInitialDialogsIfPresent(context)
    }

    @Test
    fun appLaunches_shouldShowMainScreenWithTabs() {
        // Verify app name in top bar
        composeTestRule.onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()

        // Verify help button is present
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.help_button_desc))
            .assertIsDisplayed()

        // Verify both tabs are present
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb))
            .assertIsDisplayed()

        // Verify apply settings button is present
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.button_save_apply_settings))

        Log.i(TAG, "Main screen loaded successfully with all expected elements")
    }

    @Test
    fun tabNavigation_shouldWorkCorrectly() {
        // Start on DNS tab (should be default)
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()

        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        // Verify USB settings are visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        // Navigate back to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Verify DNS settings are visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))

        Log.i(TAG, "Tab navigation works correctly")
    }

    @Test
    fun permissionDialog_shouldShowAndDismiss() {
        // Click help button to show permission dialog
        composeTestRule.clickHelpButton(context)

        // Wait for permission dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.permission_grant_dialog_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.permission_grant_dialog_title))
            .assertIsDisplayed()

        // Verify dialog content
        composeTestRule.onNodeWithText(context.getString(R.string.permission_grant_dialog_intro))
            .assertIsDisplayed()

        // Dismiss dialog
        try {
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performScrollTo()
                .performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performClick()
        }
        composeTestRule.waitForIdle()

        Log.i(TAG, "Permission dialog displayed and dismissed successfully")
    }

    @Test
    fun dnsSettings_shouldShowAllOptions() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Verify DNS settings title
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))

        // Verify tile cycles description
        composeTestRule.onNodeWithText(context.getString(R.string.setting_desc_tile_cycles))
            .assertIsDisplayed()

        // Verify DNS state options exist
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_state_off))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_state_auto))

        Log.i(TAG, "DNS settings displayed correctly")
    }

    @Test
    fun usbDebuggingSettings_shouldShowAllOptions() {
        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        // Verify USB debugging settings title
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        // Verify tile cycles description
        composeTestRule.onNodeWithText(context.getString(R.string.setting_desc_tile_cycles))
            .assertIsDisplayed()

        // Verify USB state options exist
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_on))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_off))

        Log.i(TAG, "USB debugging settings displayed correctly")
    }

    @Test
    fun applySettingsButton_shouldShowSnackbarWhenClicked() {
        // Click apply settings button
        composeTestRule.clickApplySettings(context)

        // Wait for snackbar to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.toast_settings_saved_tiles_updated))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify snackbar message is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.toast_settings_saved_tiles_updated))
            .assertIsDisplayed()

        Log.i(TAG, "Apply settings button works and shows success message")
    }

    @Test
    fun permissionWarningCard_shouldShowWhenPermissionMissing() {
        // The permission warning card should be visible by default
        // since we don't have WRITE_SECURE_SETTINGS in test environment

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.warning_permission_missing_title))

        composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_desc))
            .assertIsDisplayed()

        // Click the permission help button on the warning card
        composeTestRule.onNodeWithText(context.getString(R.string.button_show_permission_help))
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "Permission warning card displayed and button works")
    }

    @Test
    fun adbInstructionsDialog_shouldShowWhenRequested() {
        // First open permission dialog
        composeTestRule.clickHelpButton(context)
        composeTestRule.waitForIdle()

        // Look for ADB instructions button
        try {
            composeTestRule.onNodeWithText(context.getString(R.string.button_show_adb_instructions))
                .performScrollTo()
                .performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText(context.getString(R.string.button_show_adb_instructions))
                .performClick()
        }
        composeTestRule.waitForIdle()

        // Verify ADB instructions dialog appears
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.adb_instructions_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.adb_instructions_title))
            .assertIsDisplayed()

        Log.i(TAG, "ADB instructions dialog displayed successfully")
    }

    @Test
    fun multipleTabSwitches_shouldMaintainUIState() {
        // Perform multiple tab switches to ensure UI state is maintained
        repeat(3) { iteration ->
            Log.i(TAG, "Tab switch iteration ${iteration + 1}")

            // Switch to USB tab
            composeTestRule.navigateToUsbTab(context)

            // Verify USB content is visible
            composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

            // Switch to DNS tab
            composeTestRule.navigateToDnsTab(context)

            // Verify DNS content is visible
            composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))
        }

        Log.i(TAG, "Multiple tab switches completed successfully")
    }

    @Test
    fun uiElements_shouldBeAccessible() {
        // Verify accessibility of main UI elements

        // Top bar help button should have content description
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.help_button_desc))
            .assertIsDisplayed()

        // Tab navigation should be accessible
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb))
            .assertIsDisplayed()

        // Main action button should be accessible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.button_save_apply_settings))

        Log.i(TAG, "UI accessibility verification completed")
    }

    @Test
    fun dnsProviderInfoButtons_shouldOpenInfoDialogs() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Test info buttons for predefined DNS providers
        val predefinedProviders = listOf(
            "AdGuard DNS",
            "Cloudflare (1.1.1.1)",
            "Quad9 Security"
        )

        predefinedProviders.forEach { providerName ->
            Log.i(TAG, "Testing info button for: $providerName")

            // Find and click the info button for this provider
            try {
                composeTestRule.onNodeWithContentDescription(
                    context.getString(
                        R.string.button_info_dns,
                        providerName
                    )
                )
                    .performScrollTo()
                    .performClick()
            } catch (_: AssertionError) {
                composeTestRule.onNodeWithContentDescription(
                    context.getString(
                        R.string.button_info_dns,
                        providerName
                    )
                )
                    .performClick()
            }
            composeTestRule.waitForIdle()

            // Wait for info dialog to appear
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                composeTestRule.onAllNodesWithText(
                    context.getString(
                        R.string.dns_info_dialog_title,
                        providerName
                    )
                )
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify dialog is displayed
            composeTestRule.onNodeWithText(
                context.getString(
                    R.string.dns_info_dialog_title,
                    providerName
                )
            )
                .assertIsDisplayed()

            // Close dialog
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performClick()
            composeTestRule.waitForIdle()

            Log.i(TAG, "Info dialog for $providerName displayed and dismissed successfully")
        }
    }

    @Test
    fun customDnsHostname_shouldSupportFullCRUD() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        val customHostname = "Test Custom DNS"
        val customValue = "dns.example.com"
        val editedHostname = "Edited Custom DNS"
        val editedValue = "dns-edited.example.com"

        // CREATE: Add custom hostname
        Log.i(TAG, "Testing CREATE: Adding custom DNS hostname")

        // Click add button
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_add_custom_hostname_button))
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_custom_hostname_button))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify add dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_hostname_title))
            .assertIsDisplayed()

        // Fill in hostname form fields
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(customHostname)
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput(customValue)

        // Save the hostname
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // READ: Verify custom hostname appears in list
        Log.i(TAG, "Testing READ: Verifying custom hostname '$customHostname' appears in list")
        composeTestRule.safeScrollToAndAssert(customHostname)
        composeTestRule.safeScrollToAndAssert(customValue)

        // UPDATE: Edit the custom hostname
        Log.i(TAG, "Testing UPDATE: Editing custom hostname to '$editedHostname'")
        composeTestRule.onNodeWithTag("dns_edit_button_${customValue}_${customHostname}")
            .performClick()
        composeTestRule.waitForIdle()

        // Verify edit dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.dns_edit_hostname_title))
            .assertIsDisplayed()

        // Clear and update fields with edited values
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextClearance()
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(editedHostname)
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextClearance()
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput(editedValue)

        // Save the changes
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify edited hostname appears in list
        Log.i(TAG, "Verifying edited hostname '$editedHostname' appears in list")
        composeTestRule.safeScrollToAndAssert(editedHostname)
        composeTestRule.safeScrollToAndAssert(editedValue)

        // DELETE: Remove the custom hostname
        Log.i(TAG, "Testing DELETE: Deleting custom hostname '$editedHostname'")
        composeTestRule.onNodeWithTag("dns_delete_button_${editedValue}_${editedHostname}")
            .performClick()
        composeTestRule.waitForIdle()

        // Confirm deletion dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.confirm_delete_hostname_title))
            .assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithText(context.getString(R.string.button_delete))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify hostname is removed from list
        Log.i(TAG, "Verifying hostname '$editedHostname' is removed from list")
        composeTestRule.onNodeWithText(editedHostname)
            .assertDoesNotExist()

        Log.i(TAG, "Custom DNS hostname CRUD test completed successfully")
    }

    @Test
    fun dnsSettings_shouldHandleCheckboxInteractions() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing DNS checkbox interactions")

        // Test DNS Off checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.dns_state_off))
            .performClick()
        composeTestRule.waitForIdle()

        // Test DNS Auto checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.dns_state_auto))
            .performClick()
        composeTestRule.waitForIdle()

        // Test VPN Detection checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_vpn_detection_enabled))
            .performClick()
        composeTestRule.waitForIdle()

        // Test Auto-Revert checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
            .performClick()
        composeTestRule.waitForIdle()

        // Test predefined DNS provider checkbox (uncheck AdGuard DNS)
        composeTestRule.onNodeWithText("AdGuard DNS")
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun usbSettings_shouldHandleCheckboxInteractions() {
        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        Log.i(TAG, "Testing USB checkbox interactions")

        // Test USB Enable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_on))
            .performClick()
        composeTestRule.waitForIdle()

        // Test USB Disable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_off))
            .performClick()
        composeTestRule.waitForIdle()

        // Test USB Auto-Revert checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
            .performClick()
        composeTestRule.waitForIdle()
    }
}
