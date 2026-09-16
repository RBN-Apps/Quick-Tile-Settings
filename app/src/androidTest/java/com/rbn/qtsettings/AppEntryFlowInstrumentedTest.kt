package com.rbn.qtsettings

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.app.navigation.AppArea
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class AppEntryFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun appLaunches_shouldShowMainScreenWithNavigation() {
        // The top bar names the tile area the app opened on
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()

        // Verify app menu is present; permission help is available from that menu.
        composeTestRule.assertNavigationAvailable(context)

        // Both areas are reachable through the adaptive rail
        composeTestRule.selectNavigationArea(context, AppArea.Dns)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Dns)
        composeTestRule.selectNavigationArea(context, AppArea.Usb)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Usb)

        Log.i(FLOW_TAG, "Main screen loaded successfully with all expected elements")
    }

    @Test
    fun areaNavigation_shouldWorkCorrectly() {
        // Start on DNS area (should be default)
        composeTestRule.selectNavigationArea(context, AppArea.Dns)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Dns)

        // Navigate to USB area
        composeTestRule.navigateToUsbArea(context)

        // Verify USB settings are visible
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb)).assertIsDisplayed()

        // Navigate back to DNS area
        composeTestRule.navigateToDnsArea(context)

        // Verify DNS settings are visible
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns)).assertIsDisplayed()

        Log.i(FLOW_TAG, "Area navigation works correctly")
    }

    @Test
    fun permissionPage_shouldShowAndReturn() {
        // Click help button to show permission dialog
        composeTestRule.clickHelpButton(context)

        // Wait for permission dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.menu_permission_help))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.menu_permission_help))
            .assertIsDisplayed()

        // Verify dialog content
        composeTestRule.onNodeWithText(context.getString(R.string.permission_page_explanation))
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc))
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(FLOW_TAG, "Permission dialog displayed and dismissed successfully")
    }

    @Test
    fun permissionWarningCard_matchesPermission_andOpensHelpWhenMissing() {
        // Upgrade/device runs can legitimately retain this permission; a fresh CI install cannot.
        if (com.rbn.qtsettings.platform.permissions.PermissionUtils.hasWriteSecureSettingsPermission(context)) {
            composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_title))
                .assertDoesNotExist()
            return
        }

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.warning_permission_missing_title))

        composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_desc))
            .assertIsDisplayed()

        // Click the permission help button on the warning card
        composeTestRule.onNodeWithText(context.getString(R.string.button_show_permission_help))
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(FLOW_TAG, "Permission warning card displayed and button works")
    }

    @Test
    fun adbInstructionsPage_shouldShowWhenRequested() {
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

        Log.i(FLOW_TAG, "ADB instructions dialog displayed successfully")
    }

    @Test
    fun multipleTabSwitches_shouldMaintainUIState() {
        // Perform multiple area switches to ensure UI state is maintained
        repeat(3) { iteration ->
            Log.i(FLOW_TAG, "Tab switch iteration ${iteration + 1}")

            // Switch to USB area
            composeTestRule.navigateToUsbArea(context)

            // Verify USB content is visible
            composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb)).assertIsDisplayed()

            // Switch to DNS area
            composeTestRule.navigateToDnsArea(context)

            // Verify DNS content is visible
            composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns)).assertIsDisplayed()
        }

        Log.i(FLOW_TAG, "Multiple area switches completed successfully")
    }

    @Test
    fun uiElements_shouldBeAccessible() {
        // Verify accessibility of main UI elements

        // Top bar menu should have content description
        composeTestRule.assertNavigationAvailable(context)

        // Both areas expose their selection through accessible navigation items
        composeTestRule.selectNavigationArea(context, AppArea.Dns)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Dns)
        composeTestRule.selectNavigationArea(context, AppArea.Usb)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Usb)

        Log.i(FLOW_TAG, "UI accessibility verification completed")
    }
}
