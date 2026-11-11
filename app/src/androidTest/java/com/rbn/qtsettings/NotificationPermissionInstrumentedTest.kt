package com.rbn.qtsettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "NotificationPermissionTest"
private const val TAG_NETWORK_TYPE_DETECTION_TOGGLE = "network_type_detection_toggle"
private const val TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION =
    "network_type_detection_background_option"

@RunWith(AndroidJUnit4::class)
class NotificationPermissionInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun backgroundDetectionSelection_offersNotificationsOnlyAfterExplicitTap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Never `pm revoke` here: Android kills the app process on revocation, and with it the
        // instrumentation, so every later test class silently stops running. Flow tests grant
        // the permission earlier in the same process; this test then only runs on a fresh install.
        // Not an assumption: AGP exports AssumptionViolatedException as a failure.
        if (hasPostNotificationsPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS already granted in this process; nothing to verify")
            return
        }

        composeTestRule.waitForIdle()
        composeTestRule.dismissInitialDialogsIfPresent(context)
        composeTestRule.navigateToDnsAutomation(context)
        composeTestRule.safeScrollToAndAssert(
            context.getString(R.string.setting_network_type_detection_enabled)
        )

        ensureNetworkTypeDetectionEnabled()
        composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("automation_network_mode").performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            context.getString(R.string.notification_permission_background_message)
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.automation_notification_title))
            .performScrollTo().performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.notification_permission_background_message)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            context.getString(R.string.notification_permission_continue_button)
        ).performClick()
        composeTestRule.waitForIdle()

        disableNetworkTypeDetectionIfEnabled()
    }

    private fun ensureNetworkTypeDetectionEnabled() {
        val networkTypeAlreadyEnabled = composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On

        if (!networkTypeAlreadyEnabled) {
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun disableNetworkTypeDetectionIfEnabled() {
        val networkTypeEnabled = composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On

        if (networkTypeEnabled) {
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
