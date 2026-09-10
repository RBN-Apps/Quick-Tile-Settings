package com.rbn.qtsettings

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbn.qtsettings.app.AppScaffold
import com.rbn.qtsettings.app.navigation.*
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/** Dispatcher tests exercise real NavDisplay progress/ownership, not just a mutable stack. */
class PredictiveBackInstrumentedTest {
    @get:Rule val compose = createComposeRule(
        effectContext = object : MotionDurationScale { override val scaleFactor = 1f }
    )
    private lateinit var activity: ComponentActivity
    private lateinit var dialogView: android.view.View
    private val cleared = AtomicInteger()
    private val saved = AtomicInteger()
    private lateinit var focus: androidx.compose.ui.focus.FocusManager

    private fun show() {
        compose.setContent {
            activity = LocalActivity.current as ComponentActivity
            focus = androidx.compose.ui.platform.LocalFocusManager.current
            val nav = rememberAppNavigation(AppArea.Usb)
            var dialog by rememberSaveable { mutableStateOf(false) }
            QuickTileSettingsTheme {
                AppScaffold(nav.destination, nav.area, nav::selectArea,
                    remember { SnackbarHostState() }, nav::back, nav::open, {}, {}) { modifier ->
                    AppNavigationContent(nav, modifier) { route ->
                        viewModel { EntryOwner(cleared) }
                        var draft by rememberSaveable { mutableStateOf("") }
                        Column(Modifier.fillMaxSize().testTag("page_${route.destination}")) {
                            Text(route.toString())
                            BasicTextField(draft, { draft = it }, Modifier.testTag("draft_${route.destination}"))
                            Button(onClick = { nav.open(AppDestination.AppSettings) }) { Text("Settings") }
                            Button(onClick = { nav.open(AppDestination.ShortcutSettings) }) { Text("Shortcuts") }
                            Button(onClick = { dialog = true }) { Text("Edit dialog") }
                        }
                    }
                }
                if (dialog) {
                    var dialogDraft by rememberSaveable { mutableStateOf("") }
                    com.rbn.qtsettings.core.designsystem.AppDialog(
                        title = "Edit draft", onDismissRequest = { dialog = false },
                        confirmLabel = "Save draft", onConfirm = { saved.incrementAndGet(); dialog = false },
                        dismissLabel = "Cancel draft"
                    ) {
                        dialogView = androidx.compose.ui.platform.LocalView.current
                        com.rbn.qtsettings.core.designsystem.AppTextField(
                            value = dialogDraft, onValueChange = { dialogDraft = it },
                            label = "Draft", modifier = Modifier.testTag("dialog_draft")
                        )
                    }
                }
            }
        }
    }

    private fun begin(edge: Int) = compose.runOnIdle {
        activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, edge))
    }
    private fun progress(edge: Int, fraction: Float) = compose.runOnIdle {
        activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, fraction, edge))
    }
    private fun cancel() = compose.runOnIdle { activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
    private fun commit() = compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
    private fun settingsX() = compose.onNodeWithTag("page_AppSettings").fetchSemanticsNode().positionInRoot.x
    private fun settleForSystemGesture() {
        compose.waitForIdle()
        // SystemUI and the IME animate on wall-clock time outside Compose's test clock. Starting
        // another injected edge gesture while either is still settling can make SystemUI ignore it.
        android.os.SystemClock.sleep(700)
    }

    @Test fun bothEdges_previewTracksProgress_cancelRetainsDraftAndOwner_commitPopsOnce() {
        show()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("draft_AppSettings").performClick().performTextReplacement("Unsaved settings")
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            val restingX = settingsX()
            begin(edge)
            progress(edge, .2f)
            val earlyX = settingsX() - restingX
            progress(edge, .65f)
            val laterX = settingsX() - restingX
            assertTrue("Preview must follow progress", abs(laterX) > abs(earlyX) + 10f)
            assertTrue("Preview must move right from either edge", laterX > 0f)
            compose.onNodeWithTag("page_TileSettings").assertExists()
            assertEquals(0, cleared.get())
            cancel()
            compose.onNodeWithTag("draft_AppSettings").assertTextEquals("Unsaved settings").assertIsFocused()
            assertEquals(restingX, settingsX(), 1f)
            assertEquals(0, cleared.get())
        }
        begin(BackEventCompat.EDGE_LEFT)
        progress(BackEventCompat.EDGE_LEFT, .8f)
        commit()
        compose.onNodeWithTag("page_TileSettings").assertIsDisplayed()
        compose.onNodeWithTag("page_AppSettings").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, cleared.get()) }
    }

    @Test fun repeatedCancelledGesturesAndGlobalFlow_returnToCallerWithoutDoublePop() {
        show()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("draft_AppSettings").performTextReplacement("Caller draft")
        compose.onNodeWithText("Shortcuts").performClick()
        repeat(3) {
            begin(BackEventCompat.EDGE_RIGHT)
            progress(BackEventCompat.EDGE_RIGHT, .4f)
            cancel()
        }
        begin(BackEventCompat.EDGE_RIGHT)
        progress(BackEventCompat.EDGE_RIGHT, .7f)
        commit()
        compose.onNodeWithTag("draft_AppSettings").assertTextEquals("Caller draft")
        compose.runOnIdle { assertEquals(1, cleared.get()) }
        begin(BackEventCompat.EDGE_LEFT)
        progress(BackEventCompat.EDGE_LEFT, .6f)
        commit()
        compose.onNodeWithTag("page_TileSettings").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, cleared.get()) }
    }

    @Test fun realEdgeGestures_previewCancelAndCommitFromBothEdges() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 34)
        org.junit.Assert.assertTrue("Enable gestural navigation before running real edge-gesture tests", android.provider.Settings.Secure.getInt(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
            "navigation_mode", 0) == 2)
        show()
        compose.waitForAppWindowFocus()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("draft_AppSettings").performTextReplacement("Keep on cancel")
        compose.runOnIdle {
            focus.clearFocus()
            androidx.core.view.WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == false
        }
        for (right in listOf(false, true)) {
            settleForSystemGesture()
            val restingX = settingsX()
            SystemBackGesture(activity.window.decorView.width, activity.window.decorView.height, right).use { gesture ->
                gesture.begin()
                compose.waitUntil(5_000) { settingsX() - restingX > 10f }
                gesture.cancel()
            }
            compose.onNodeWithTag("draft_AppSettings").assertTextEquals("Keep on cancel")
            compose.waitUntil(5_000) { abs(settingsX() - restingX) < 1f }
        }
        settleForSystemGesture()
        SystemBackGesture(activity.window.decorView.width, activity.window.decorView.height, false).use {
            it.begin()
            it.commit()
        }
        compose.onNodeWithTag("page_TileSettings").assertIsDisplayed()
        compose.waitUntil(5_000) { cleared.get() == 1 }
    }

    @Test fun realGesture_failedPreviewReleasesPointerForNextGesture() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 34)
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(2, android.provider.Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0))
        show()
        compose.waitForAppWindowFocus()
        compose.onNodeWithText("Settings").performClick()
        settleForSystemGesture()
        val restingX = settingsX()
        val expected = AssertionError("Simulated assertion failure while the finger is held")
        try {
            SystemBackGesture(activity.window.decorView.width, activity.window.decorView.height, false).use {
                it.begin()
                compose.waitUntil(5_000) { settingsX() - restingX > 10f }
                throw expected
            }
        } catch (failure: AssertionError) {
            assertSame(expected, failure)
        }
        compose.waitUntil(5_000) { abs(settingsX() - restingX) < 1f }
        compose.runOnIdle { assertEquals(0, cleared.get()) }
        settleForSystemGesture()
        SystemBackGesture(activity.window.decorView.width, activity.window.decorView.height, true).use {
            it.begin()
            compose.waitUntil(5_000) { settingsX() - restingX > 10f }
            it.commit()
        }
        compose.onNodeWithTag("page_TileSettings").assertIsDisplayed()
        compose.waitUntil(5_000) { cleared.get() == 1 }
    }

    @Test fun realGestures_imeThenDialogThenRailThenPage_noSaveOrExtraPop() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 34)
        org.junit.Assert.assertTrue("Enable gestural navigation before running real edge-gesture tests", android.provider.Settings.Secure.getInt(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
            "navigation_mode", 0) == 2)
        show()
        compose.waitForAppWindowFocus()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Edit dialog").performClick()
        compose.waitForDialogWindowFocus()
        compose.onNodeWithTag("dialog_draft").performTouchInput { click() }
        compose.onNodeWithTag("dialog_draft").performTextReplacement("Do not save")
        // Dialogs have a separate window; query its view rather than the Activity's insets.
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(dialogView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        }
        fun gesture() = SystemBackGesture(activity.window.decorView.width, activity.window.decorView.height, false)
        settleForSystemGesture()
        gesture().use { it.begin(); it.commit() }
        compose.onNodeWithText("Edit draft").assertExists()
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(dialogView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithTag("dialog_draft").assertTextContains("Do not save")
        compose.waitForDialogWindowFocus()
        settleForSystemGesture()
        gesture().use { it.begin(); it.commit() }
        // Android delivers the dialog-window dismissal asynchronously after touch injection.
        // Wait for that observable result; Compose idle alone does not drain the system callback.
        compose.waitUntil(5_000) {
            compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Edit draft").assertDoesNotExist()
        compose.onNodeWithTag("page_AppSettings").assertIsDisplayed()
        assertEquals(0, saved.get())
        compose.waitForAppWindowFocus()
        // Persistent rails remain visible on wide windows; only modal rails consume Back.
        if (activity.resources.configuration.screenWidthDp < 600) {
            compose.onNodeWithContentDescription(activity.getString(R.string.navigation_open)).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("modal_navigation_open").fetchSemanticsNodes().isNotEmpty()
            }
            begin(BackEventCompat.EDGE_RIGHT)
            progress(BackEventCompat.EDGE_RIGHT, .65f)
            cancel()
            compose.onNodeWithTag("navigation_area_Usb").assertIsDisplayed()
            begin(BackEventCompat.EDGE_RIGHT)
            progress(BackEventCompat.EDGE_RIGHT, .65f)
            commit()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("navigation_area_Usb").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithTag("page_AppSettings").assertIsDisplayed()
            assertEquals(0, cleared.get())
        }
        compose.waitForAppWindowFocus()
        settleForSystemGesture()
        gesture().use { it.begin(); it.commit() }
        compose.onNodeWithTag("page_TileSettings").assertIsDisplayed()
        // NavDisplay removes the outgoing entry before its ViewModel store is cleared.
        compose.waitUntil(5_000) { cleared.get() == 1 }
    }

    private class EntryOwner(private val cleared: AtomicInteger) : ViewModel() {
        override fun onCleared() { cleared.incrementAndGet() }
    }
}
