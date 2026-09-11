package com.rbn.qtsettings

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rbn.qtsettings.app.navigation.*
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState
import com.rbn.qtsettings.feature.usb.presentation.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UsbPilotInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var activity: ComponentActivity
    private var delay = 5
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun activateAction(stateLabel: Int) = compose.onNodeWithContentDescription(
        context.getString(R.string.usb_set_active_mode, context.getString(stateLabel))
    )

    @Composable
    private fun Harness() {
        activity = LocalActivity.current as ComponentActivity
        val nav = rememberAppNavigation(AppArea.Usb)
        var state by remember {
            mutableStateOf(
                UsbSettingsState(
                    system = UsbSystemState(true, true, false),
                    hasPermission = true
                )
            )
        }
        val action: (UsbSettingsAction) -> Unit = {
            when (it) {
                is UsbSettingsAction.EnableAutoRevert -> state =
                    state.copy(settings = state.settings.copy(enableAutoRevert = it.value))

                is UsbSettingsAction.AutoRevertDelaySeconds -> {
                    delay = it.value; state =
                        state.copy(settings = state.settings.copy(autoRevertDelaySeconds = it.value))
                }

                else -> Unit
            }
        }
        QuickTileSettingsTheme {
            Column {
                Button(onClick = { nav.selectArea(AppArea.Dns) }) { Text("DNS area") }
                Button(onClick = { nav.selectArea(AppArea.Usb) }) { Text("USB area") }
                Button(onClick = nav::back) { Text("Back") }
                AppNavigationContent(nav, Modifier.weight(1f)) { route ->
                    when (route) {
                        AppRoute.Usb -> UsbSettingsScreen(
                            state,
                            action,
                            { nav.open(AppDestination.UsbTile) },
                            { nav.open(AppDestination.UsbActions) },
                            { nav.open(AppDestination.UsbRevert) })

                        AppRoute.UsbTile -> UsbTileScreen(state, action)
                        AppRoute.UsbActions -> UsbAdditionalActionsScreen(state, action)
                        AppRoute.UsbRevert -> UsbAutoRevertScreen(state, action)
                        else -> Text("DNS root")
                    }
                }
            }
        }
    }

    @Test
    fun detailsSurviveAreaSwitchAndRestore_predictiveCancelAndCommitReturnToOverview() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Harness() }
        text(R.string.usb_menu_tile).performClick()
        activateAction(R.string.usb_state_off).assertIsDisplayed()
        compose.onNodeWithText("DNS area").performClick()
        compose.onNodeWithText("USB area").performClick()
        restoration.emulateSavedInstanceStateRestore()
        text(R.string.usb_state_off).assertIsDisplayed()
        compose.runOnIdle {
            activity.onBackPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(
                    0f,
                    0f,
                    0f,
                    BackEventCompat.EDGE_LEFT
                )
            )
            activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(
                    100f,
                    0f,
                    .6f,
                    BackEventCompat.EDGE_LEFT
                )
            )
        }
        compose.runOnIdle { activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        text(R.string.usb_state_off).assertIsDisplayed()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        text(R.string.usb_menu_actions).performClick()
        text(R.string.setting_also_hide_dev_options).assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        text(R.string.setting_require_unlock).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cycleSelectionAndImmediateActionsRemainIndependent() {
        val actions = mutableListOf<UsbSettingsAction>()
        val state = UsbSettingsState(system = UsbSystemState(true, true, false), hasPermission = true)
        compose.setContent { QuickTileSettingsTheme { UsbTileScreen(state, actions::add) } }
        text(R.string.usb_state_on).performClick()
        compose.runOnIdle {
            assertEquals(listOf(UsbSettingsAction.ToggleEnable(!state.settings.toggleEnable)), actions)
            actions.clear()
        }
        // The active badge marks the observed state; only the row action changes it.
        compose.onNodeWithContentDescription(context.getString(R.string.usb_state_active))
            .assertIsDisplayed()
        activateAction(R.string.usb_state_on).assertIsNotEnabled()
        activateAction(R.string.usb_state_off).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(UsbSettingsAction.SetActive(false)), actions) }
    }

    @Test
    fun largeOverviewAndCombinedAlternative_keepActionsReachable() {
        var combined by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                if (combined) UsbCombinedPilotPreview()
                else QuickTileSettingsTheme { UsbSettingsScreen(UsbSettingsState(), {}) }
            }
        }
        capture("large-overview")
        text(R.string.setting_require_unlock).performScrollTo().assertIsDisplayed()
        capture("large-overview-scrolled")
        compose.runOnIdle { combined = true }
        capture("combined-alternative")
        text(R.string.setting_also_disable_wireless_debugging).performScrollTo().assertIsDisplayed()
        capture("combined-alternative-scrolled")
    }

    private fun capture(name: String) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments()
                .getString("recordUsbPilot") != "true"
        ) return
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(300) // Allow the real window to present the optional screenshot.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun revertPageKeepsTheTimerContractBehindItsOwnRow() {
        compose.setContent { Harness() }
        text(R.string.usb_menu_revert).performClick()
        // The page shows settings only; the contract prose lives in the dialog.
        assertEquals(0, explanationNodes().fetchSemanticsNodes().size)
        text(R.string.usb_revert_details).performClick()
        text(R.string.usb_revert_explanation).assertIsDisplayed()
        text(R.string.usb_revert_cancellation).assertExists()
        text(R.string.dialog_close).performClick()
        compose.waitUntil { explanationNodes().fetchSemanticsNodes().isEmpty() }
    }

    private fun explanationNodes() =
        compose.onAllNodesWithText(context.getString(R.string.usb_revert_explanation))

    @Test
    fun delayDialogCancelPreservesValue_invalidValueCannotSave_draftSurvivesRestore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Harness() }
        text(R.string.usb_menu_revert).performClick()
        text(R.string.setting_auto_revert_delay).assertIsNotEnabled()
        text(R.string.setting_enable_auto_revert).performClick()
        text(R.string.setting_auto_revert_delay).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("12")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("12")
        text(android.R.string.cancel).performClick()
        assertEquals(5, delay)
        // The harness configuration resets on process recreation. Enable it again if necessary.
        if (compose.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
                .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.Off
        ) {
            text(R.string.setting_enable_auto_revert).performClick()
        }
        text(R.string.setting_auto_revert_delay).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        text(R.string.usb_apply).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("12")
        text(R.string.usb_apply).performClick()
        assertEquals(12, delay)
    }
}
