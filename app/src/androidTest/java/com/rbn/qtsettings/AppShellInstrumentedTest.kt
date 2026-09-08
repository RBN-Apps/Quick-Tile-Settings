package com.rbn.qtsettings

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService
import com.rbn.qtsettings.viewmodel.MainViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppShellInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var previousHelpShown = false

    @Before fun prepare() {
        // Semantic clicks do not switch Android out of keyboard navigation mode.
        instrumentation.setInTouchMode(true)
        val preferences = PreferencesManager.getInstance(context)
        previousHelpShown = preferences.helpShown.value
        preferences.setHelpShown(true)
    }

    @After fun close() {
        if (::scenario.isInitialized) scenario.close()
        PreferencesManager.getInstance(context).setHelpShown(previousHelpShown)
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
        icon(R.string.app_menu_desc).performClick()
        text(R.string.menu_settings).performClick()
    }
    private fun usbIntent() = Intent(context, MainActivity::class.java)
        .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(context, UsbDebuggingTileService::class.java))

    @Test fun toolbarAndSystemBackReturnToUsbWithScrollPosition() {
        launch()
        text(R.string.tab_title_usb).performClick()
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
        icon(R.string.shortcut_settings_open_desc).performClick()
        text(R.string.shortcut_settings_title).assertIsDisplayed()
        icon(R.string.navigate_back_desc).performClick()
        text(R.string.tab_title_usb).assertIsSelected()
        unlock.assertIsDisplayed()
        assertEquals(before, offset(), 1f)
        openSettings()
        text(R.string.app_settings_title).assertIsDisplayed()
        // Save while the pager is absent, then restore before returning to its scroll position.
        scenario.recreate()
        text(R.string.app_settings_title).assertIsDisplayed()
        systemBack()
        text(R.string.tab_title_usb).assertIsSelected()
        unlock.assertIsDisplayed()
        assertEquals(before, offset(), 1f)
    }

    @Test fun globalDestinationAndAreaSurviveRecreationAndRotation_withOneFeatureOwner() {
        launch()
        text(R.string.tab_title_usb).performClick()
        icon(R.string.shortcut_settings_open_desc).performClick()
        lateinit var owner: MainViewModel
        scenario.onActivity { owner = ViewModelProvider(it)[MainViewModel::class.java] }
        scenario.recreate()
        text(R.string.shortcut_settings_title).assertIsDisplayed()
        scenario.onActivity {
            assertSame(owner, ViewModelProvider(it)[MainViewModel::class.java])
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(10_000) { context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        try {
            text(R.string.shortcut_settings_title).assertIsDisplayed()
            systemBack()
            text(R.string.tab_title_usb).assertIsSelected()
        } finally {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test fun coldTileEntryIsNotReappliedAfterResumeOrRecreation() {
        launch(usbIntent())
        text(R.string.tab_title_usb).assertIsSelected()
        text(R.string.tab_title_dns).performClick()
        openSettings()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        text(R.string.app_settings_title).assertIsDisplayed()
        scenario.recreate()
        text(R.string.app_settings_title).assertIsDisplayed()
        systemBack()
        text(R.string.tab_title_dns).assertIsSelected()
    }

    @Test fun repeatedWarmTileIntentsSelectTheirAreaWithoutAddingHistory() {
        launch()
        repeat(2) {
            icon(R.string.shortcut_settings_open_desc).performClick()
            // Deliver through Android's real onNewIntent path to the existing Activity.
            scenario.onActivity {
                it.startActivity(usbIntent().addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            compose.waitForIdle()
            text(R.string.tab_title_usb).assertIsSelected()
        }
        scenario.onActivity {
            it.startActivity(Intent(it, MainActivity::class.java)
                .putExtra(Intent.EXTRA_COMPONENT_NAME, PrivateDnsTileService::class.java.name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        compose.waitForIdle()
        text(R.string.tab_title_dns).assertIsSelected()
        systemBack()
        compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
    }
}
