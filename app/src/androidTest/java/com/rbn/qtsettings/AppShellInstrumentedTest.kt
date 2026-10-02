package com.rbn.qtsettings

import androidx.test.filters.SdkSuppress
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@AdaptiveLayout
class AppShellInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var previousHelpShown = false
    private var previousOnboarding = false

    @Before fun prepare() {
        // Semantic clicks do not switch Android out of keyboard navigation mode.
        instrumentation.setInTouchMode(true)
        val preferences = AppStores.getInstance(context)
        previousHelpShown = preferences.app.helpShown.value
        previousOnboarding = preferences.app.onboardingCompleted.value
        preferences.app.setHelpShown(true)
        preferences.app.setOnboardingCompleted(true)
    }

    @After fun close() {
        if (::scenario.isInitialized) scenario.close()
        AppStores.getInstance(context).app.setHelpShown(previousHelpShown)
        AppStores.getInstance(context).app.setOnboardingCompleted(previousOnboarding)
    }

    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun icon(id: Int) = compose.onNodeWithContentDescription(context.getString(id))
    private fun launch(intent: Intent = Intent(context, MainActivity::class.java)) {
        scenario = ActivityScenario.launch(intent)
        compose.waitForIdle()
    }
    private fun systemBack() {
        compose.waitForAppWindowFocus()
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK")
        ).use { it.readBytes() }
        compose.waitForIdle()
    }
    private fun openSettings() {
        compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.Settings)
    }
    private fun usbIntent() = Intent(context, MainActivity::class.java)
        .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(context, UsbDebuggingTileService::class.java))

    @Test fun toolbarAndSystemBackReturnToUsbWithScrollPosition() {
        launch()
        compose.selectNavigationArea(context, AppArea.Usb)
        val unlock = text(R.string.setting_require_unlock)
        unlock.performScrollTo().assertIsDisplayed()
        val scroll = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                hasAnyDescendant(hasText(context.getString(R.string.setting_require_unlock)))
        )
        fun offset() = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        // Measure content scrolling, not screen coordinates: Android can dispatch new system-bar
        // insets after recreation without changing the saved scroll position.
        val before = offset()
        compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.Shortcuts)
        text(R.string.shortcut_settings_title).assertIsDisplayed()
        icon(R.string.navigate_back_desc).performClick()
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        unlock.assertIsDisplayed()
        assertEquals(before, offset(), 1f)
        openSettings()
        text(R.string.app_settings_title).assertIsDisplayed()
        // Save while the pager is absent, then restore before returning to its scroll position.
        scenario.recreate()
        text(R.string.app_settings_title).assertIsDisplayed()
        systemBack()
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        unlock.assertIsDisplayed()
        assertEquals(before, offset(), 1f)
    }

    @Test fun globalDestinationAndAreaSurviveRecreationAndRotation_withOneFeatureOwner() {
        launch()
        rotateTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT)
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.Shortcuts)
        lateinit var owner: AppPermissionViewModel
        scenario.onActivity { owner = ViewModelProvider(it)[AppPermissionViewModel::class.java] }
        scenario.recreate()
        text(R.string.shortcut_settings_title).assertIsDisplayed()
        scenario.onActivity {
            assertSame(owner, ViewModelProvider(it)[AppPermissionViewModel::class.java])
        }
        rotateTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE)
        scenario.onActivity {
            assertSame(owner, ViewModelProvider(it)[AppPermissionViewModel::class.java])
        }
        text(R.string.shortcut_settings_title).assertIsDisplayed()
        systemBack()
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        // @After closes the Activity (and its orientation request). Calling onActivity in a
        // finally block would mask a failed Back assertion if Back unexpectedly finished it.
    }

    private fun rotateTo(requestedOrientation: Int, expectedOrientation: Int) {
        lateinit var previous: MainActivity
        scenario.onActivity { previous = it }
        val alreadyOriented = compose.runOnUiThread {
            previous.resources.configuration.orientation == expectedOrientation
        }
        scenario.onActivity { it.requestedOrientation = requestedOrientation }
        compose.waitUntil(10_000) {
            compose.runOnUiThread {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .any { activity ->
                        (alreadyOriented || activity !== previous) &&
                            activity.resources.configuration.orientation == expectedOrientation &&
                            activity.window.decorView.hasWindowFocus()
                    }
            }
        }
        // Application resources can change while the old Activity/Compose tree is still alive.
        // Wait for the replacement first, then drain its composition and Back registration.
        compose.waitForIdle()
        compose.waitForAppWindowFocus()
    }

    @Test fun coldTileEntryIsNotReappliedAfterResumeOrRecreation() {
        launch(usbIntent())
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        compose.selectNavigationArea(context, AppArea.Dns)
        openSettings()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        text(R.string.app_settings_title).assertIsDisplayed()
        scenario.recreate()
        text(R.string.app_settings_title).assertIsDisplayed()
        systemBack()
        compose.assertNavigationAreaSelected(context, AppArea.Dns)
    }

    @Test fun systemBackAtUsbRootExitsDirectly() {
        launch()
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        systemBack()
        compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
    }

    @Test fun repeatedWarmTileIntentsSelectTheirAreaWithoutAddingHistory() {
        launch()
        repeat(2) { round ->
            compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.Shortcuts)
            // Deliver through Android's real onNewIntent path to the existing Activity.
            deliverWarmIntent(usbIntent(), "usb-$round")
            compose.assertNavigationAreaSelected(context, AppArea.Usb)
        }
        deliverWarmIntent(Intent(context, MainActivity::class.java)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, PrivateDnsTileService::class.java.name), "dns")
        compose.assertNavigationAreaSelected(context, AppArea.Dns)
        systemBack()
        compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
    }

    /** startActivity() returns before onNewIntent runs; wait until the Activity holds this intent. */
    private fun deliverWarmIntent(intent: Intent, marker: String) {
        scenario.onActivity {
            it.startActivity(intent.putExtra(WARM_INTENT_MARKER, marker)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        compose.waitUntil(5_000) {
            var delivered = false
            scenario.onActivity { delivered = it.intent.getStringExtra(WARM_INTENT_MARKER) == marker }
            delivered
        }
        compose.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test fun realBackGestures_cancelCommitAndHome_preserveUsbScroll() {
        org.junit.Assert.assertTrue("Enable gestural navigation before running real edge-gesture tests", android.provider.Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0) == 2)
        launch(usbIntent())
        text(R.string.setting_require_unlock).performScrollTo()
        val scroll = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                hasAnyDescendant(hasText(context.getString(R.string.setting_require_unlock)))
        )
        val offset = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        var width = 0
        var height = 0
        scenario.onActivity { width = it.window.decorView.width; height = it.window.decorView.height }
        fun gesture(right: Boolean = false): SystemBackGesture {
            compose.waitForIdle()
            return SystemBackGesture(width, height, right)
        }
        fun capture(name: String) {
            // Drain Compose's test clock before capturing a real window; wall-clock sleep alone
            // leaves semantic-click transitions frozen (for example the closing overflow menu).
            compose.waitForIdle()
            if (InstrumentationRegistry.getArguments().getString("recordPredictiveBack") != "true") return
            android.os.SystemClock.sleep(700)
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        openSettings()
        icon(R.string.navigate_back_desc).assertIsDisplayed()
        capture("126-settings")
        gesture().use { gesture -> gesture.begin(); capture("126-left-preview"); gesture.cancel() }
        text(R.string.app_settings_title).assertIsDisplayed()
        capture("126-cancelled")
        gesture(true).use { gesture -> gesture.begin(); capture("126-right-preview"); gesture.commit() }
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
        text(R.string.setting_require_unlock).assertIsDisplayed()
        assertEquals(offset, scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(), 1f)
        capture("126-returned-usb")
        gesture().use { gesture -> gesture.begin(); capture("126-home-preview"); gesture.commit() }
        compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
    }

}

private const val WARM_INTENT_MARKER = "test.warm_intent_marker"
