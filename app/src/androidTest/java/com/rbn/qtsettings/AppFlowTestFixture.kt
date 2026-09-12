package com.rbn.qtsettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import org.junit.Before
import org.junit.Rule

internal const val FLOW_TAG = "QuickTileSettingsTest"
internal const val FLOW_TAG_NETWORK_TYPE_DETECTION_TILE_OPTION = "network_type_detection_tile_only_option"
internal const val FLOW_TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION =
    "network_type_detection_background_option"
internal const val FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE = "network_type_detection_toggle"
internal const val FLOW_TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL =
    "network_type_detection_wifi_state_label"
internal const val FLOW_TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL =
    "network_type_detection_mobile_state_label"

abstract class AppFlowTestFixture {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    protected lateinit var context: Context
    protected var hasPostNotificationsPermission = false
    protected var canUseBackgroundDetection = false

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.waitForIdle()

        grantPostNotificationsPermissionIfNeeded()
        composeTestRule.dismissInitialDialogsIfPresent(context)

        canUseBackgroundDetection =
            hasPostNotificationsPermission && PermissionUtils.hasWriteSecureSettingsPermission(
                context
            )
    }

    @OptIn(ExperimentalTestApi::class)

    protected fun networkDetectionIsEnabled() = composeTestRule.onNodeWithTag(FLOW_TAG_NETWORK_TYPE_DETECTION_TOGGLE)
        .fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On

    protected fun ensureNetworkTypeDetectionEnabled() {
        val networkTypeAlreadyEnabled = networkDetectionIsEnabled()

        if (!networkTypeAlreadyEnabled) {
            Log.i(FLOW_TAG, "Enabling Network Type Detection")
            composeTestRule.onNodeWithText(context.getString(R.string.setting_network_type_detection_enabled))
                .performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500) // Give animation time to complete
        } else {
            Log.i(FLOW_TAG, "Network Type Detection already enabled")
        }
    }

    protected fun grantPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            hasPostNotificationsPermission = true
            return
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val packageName = instrumentation.targetContext.packageName

        val alreadyGranted = ContextCompat.checkSelfPermission(
            instrumentation.targetContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            hasPostNotificationsPermission = true
            Log.i(FLOW_TAG, "POST_NOTIFICATIONS permission already granted")
            return
        }

        runCatching {
            instrumentation.uiAutomation.executeShellCommand("pm grant $packageName $permission")
                .use { fd ->
                    drainCommandOutput(fd)
                }
            instrumentation.waitForIdleSync()
        }.onFailure { error ->
            Log.w(FLOW_TAG, "Failed to grant POST_NOTIFICATIONS permission: ${error.message}")
        }

        hasPostNotificationsPermission = ContextCompat.checkSelfPermission(
            instrumentation.targetContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    protected fun drainCommandOutput(fd: ParcelFileDescriptor) {
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                while (reader.readLine() != null) {
                    // Drain output to ensure command completes
                }
            }
        }
    }
}
