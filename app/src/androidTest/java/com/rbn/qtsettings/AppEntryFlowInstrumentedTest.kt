package com.rbn.qtsettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke tests of the installed app's entry screen; navigation details live in the shell tests. */
@AdaptiveLayout
@RunWith(AndroidJUnit4::class)
class AppEntryFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun appLaunchesOnDns_andBothAreasAreReachable() {
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns)).assertIsDisplayed()
        composeTestRule.assertNavigationAvailable(context)

        composeTestRule.selectNavigationArea(context, AppArea.Usb)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Usb)
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb)).assertIsDisplayed()

        composeTestRule.selectNavigationArea(context, AppArea.Dns)
        composeTestRule.assertNavigationAreaSelected(context, AppArea.Dns)
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns)).assertIsDisplayed()
    }

    @Test
    fun permissionWarningCard_matchesPermission_andOpensHelpWhenMissing() {
        // Upgrade/device runs can legitimately retain this permission; a fresh CI install cannot.
        if (PermissionUtils.hasWriteSecureSettingsPermission(context)) {
            composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_title))
                .assertDoesNotExist()
            return
        }

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.warning_permission_missing_title))
        composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_desc))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.button_show_permission_help))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.permission_page_explanation))
            .assertIsDisplayed()
    }
}
