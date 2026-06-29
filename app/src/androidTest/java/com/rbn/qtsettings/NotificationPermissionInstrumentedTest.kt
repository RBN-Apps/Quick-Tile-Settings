package com.rbn.qtsettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.BufferedReader
import java.io.InputStreamReader

private const val NOTIFICATION_PERMISSION_TEST_TAG = "NotificationPermissionTest"
private const val TAG_NETWORK_TYPE_DETECTION_TOGGLE = "network_type_detection_toggle"
private const val TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION =
    "network_type_detection_background_option"

@RunWith(AndroidJUnit4::class)
class NotificationPermissionInstrumentedTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(PostNotificationsDeniedRule())
        .around(composeTestRule)

    @Test
    fun backgroundDetectionSelection_withoutNotificationPermission_showsExplanationDialog() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        assumeTrue(!hasPostNotificationsPermission(context))

        composeTestRule.waitForIdle()
        composeTestRule.dismissInitialDialogsIfPresent(context)
        composeTestRule.navigateToDnsTab(context)
        composeTestRule.safeScrollToAndAssert(
            context.getString(R.string.setting_network_type_detection_enabled)
        )

        ensureNetworkTypeDetectionEnabled(context)
        composeTestRule.safeScrollToAndAssert(
            context.getString(R.string.setting_network_type_detection_mode)
        )

        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            context.getString(R.string.notification_permission_background_message)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            context.getString(R.string.notification_permission_use_tile_only_button)
        ).performClick()
        composeTestRule.waitForIdle()

        disableNetworkTypeDetectionIfEnabled(context)
    }

    private fun ensureNetworkTypeDetectionEnabled(context: Context) {
        val networkTypeAlreadyEnabled = composeTestRule.onAllNodesWithText(
            context.getString(R.string.setting_dns_state_on_wifi)
        ).fetchSemanticsNodes().isNotEmpty()

        if (!networkTypeAlreadyEnabled) {
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun disableNetworkTypeDetectionIfEnabled(context: Context) {
        val networkTypeEnabled = composeTestRule.onAllNodesWithText(
            context.getString(R.string.setting_dns_state_on_wifi)
        ).fetchSemanticsNodes().isNotEmpty()

        if (networkTypeEnabled) {
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
}

private class PostNotificationsDeniedRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setPostNotificationsPermission(granted = false)
                }
                try {
                    base.evaluate()
                } finally {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setPostNotificationsPermission(granted = true)
                    }
                }
            }
        }
    }

    private fun setPostNotificationsPermission(granted: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val command = if (granted) {
            "pm grant $packageName $permission"
        } else {
            "pm revoke $packageName $permission"
        }

        runCatching {
            instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
                drainCommandOutput(fd)
            }
            instrumentation.waitForIdleSync()
        }.onFailure { error ->
            Log.w(
                NOTIFICATION_PERMISSION_TEST_TAG,
                "Failed to update POST_NOTIFICATIONS permission: ${error.message}"
            )
        }
    }

    private fun drainCommandOutput(fd: ParcelFileDescriptor) {
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                while (reader.readLine() != null) {
                    // Drain output to ensure command completes
                }
            }
        }
    }
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
