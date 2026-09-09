package com.rbn.qtsettings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppScaffold
import com.rbn.qtsettings.app.navigation.*
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppNavigationRailInstrumentedTest {
    @get:Rule val compose = createComposeRule(
        // Animation assertions must run at normal speed even when CI disables system animations.
        effectContext = object : MotionDurationScale { override val scaleFactor = 1f }
    )
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var width by mutableIntStateOf(390)

    private fun show(restoration: StateRestorationTester? = null, fontScale: Float = 1f) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            // Use actual layout constraints at a predictable density. No production breakpoint
            // override: resizing this container exercises the same measurement as split-screen.
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                QuickTileSettingsTheme {
                    val nav = rememberAppNavigation()
                    Box(Modifier.requiredWidth(width.dp)) {
                        AppScaffold(nav.destination, nav.area, nav::selectArea,
                            remember { SnackbarHostState() }, nav::back, nav::open, {}, {}) { modifier ->
                            AppNavigationContent(nav, modifier.testTag("screen")) { route ->
                                var draft by rememberSaveable { mutableStateOf("") }
                                Column {
                                    Text(route.toString())
                                    BasicTextField(draft, { draft = it }, Modifier.testTag("draft"))
                                    Button(onClick = { nav.open(AppDestination.AppSettings) }) { Text("Edit settings") }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun icon(id: Int) = compose.onNodeWithContentDescription(context.getString(id))
    private fun resize(value: Int) { compose.runOnIdle { width = value } }
    private fun draft(value: String) { compose.onNodeWithTag("draft").performTextReplacement(value) }
    private fun assertDraft(value: String) { compose.onNodeWithTag("draft").assertTextEquals(value) }

    @Test fun resizeDefaultsAndManualToggle_preserveBothAreaStacksAndDrafts() {
        val restoration = StateRestorationTester(compose)
        show(restoration)
        compose.onNodeWithTag("persistent_navigation_rail").assertDoesNotExist()
        compose.onNodeWithTag("screen").assertWidthIsEqualTo(390.dp)
        draft("DNS root draft")
        compose.onNodeWithTag("screen").performTouchInput { swipeLeft() }
        assertDraft("DNS root draft")
        compose.onNodeWithText("Edit settings").performClick()
        draft("DNS draft")
        // Compact navigation stays reachable alongside Up on a detail screen.
        icon(R.string.navigate_back_desc).assertIsDisplayed()
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.onNodeWithTag("navigation_area_Usb").assertDoesNotExist()
        compose.onNodeWithText("Edit settings").performClick()
        draft("USB draft")
        resize(700)
        icon(R.string.navigation_expand).assertIsDisplayed().performClick()
        resize(750) // Same breakpoint must retain manual expansion.
        icon(R.string.navigation_collapse).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        icon(R.string.navigation_collapse).assertIsDisplayed()
        assertDraft("USB draft")
        icon(R.string.navigation_collapse).performClick()
        resize(1000) // Entering expanded resets its presentation default.
        icon(R.string.navigation_collapse).assertIsDisplayed().performClick()
        resize(1050)
        icon(R.string.navigation_expand).assertIsDisplayed()
        compose.selectNavigationArea(context, AppArea.Dns)
        assertDraft("DNS draft")
        compose.selectNavigationArea(context, AppArea.Dns) // Reselect is a no-op.
        assertDraft("DNS draft")
        resize(390)
        compose.onNodeWithTag("screen").assertWidthIsEqualTo(390.dp)
        compose.selectNavigationArea(context, AppArea.Usb)
        assertDraft("USB draft")
    }

    @Test fun compactBackDismissesRailBeforeDetail_andDoesNotChangeArea() {
        show()
        compose.onNodeWithText("Edit settings").performClick()
        icon(R.string.navigation_open).performClick()
        compose.onNodeWithTag("navigation_area_Dns").assertIsSelected()
        Espresso.pressBack()
        compose.onNodeWithTag("navigation_area_Dns").assertDoesNotExist()
        icon(R.string.navigate_back_desc).assertIsDisplayed().performClick()
        icon(R.string.navigate_back_desc).assertDoesNotExist()
        compose.assertNavigationAreaSelected(context, AppArea.Dns)
    }

    @Test fun largeFont_destinationsRemainReachableAndHaveAccessibleTouchTargets() {
        width = 700
        show(fontScale = 2f)
        compose.onNodeWithTag("navigation_area_Usb")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            .assertContentDescriptionEquals(context.getString(R.string.tab_title_usb))
        icon(R.string.navigation_expand).performClick()
        compose.onNodeWithText(context.getString(R.string.tab_title_usb)).assertIsDisplayed()
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.onNodeWithTag("navigation_area_Usb").assertIsSelected()
    }

    @Test fun menuAlignmentAndSpacing_matchToolbarAndRemainStableWhenExpanding() {
        show(fontScale = 1.5f)
        val toolbarY = icon(R.string.navigation_open).fetchSemanticsNode().boundsInRoot.center.y
        icon(R.string.navigation_open).performClick()
        val menu = icon(R.string.navigation_collapse).fetchSemanticsNode().boundsInRoot
        assertEquals(toolbarY, menu.center.y, 1f)
        val first = compose.onNodeWithTag("navigation_area_Dns").fetchSemanticsNode().boundsInRoot
        assertTrue("Compact gap below menu: ${first.top - menu.bottom}", first.top - menu.bottom <= 24f)
        icon(R.string.navigation_collapse).performClick()
        resize(700)
        val narrowY = icon(R.string.navigation_expand).fetchSemanticsNode().boundsInRoot.center.y
        icon(R.string.navigation_expand).performClick()
        assertEquals(narrowY, icon(R.string.navigation_collapse).fetchSemanticsNode().boundsInRoot.center.y, 1f)
    }

    @Test fun closingModal_keepsFullLabelsForEveryAnimationFrame_afterSelectionAndManualClose() {
        show()
        listOf(false, true).forEach { selectDestination ->
            icon(R.string.navigation_open).performClick()
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            try {
                if (selectDestination) compose.onNodeWithTag("navigation_area_Usb").performClick()
                else icon(R.string.navigation_collapse).performClick()
                var visibleFrames = 0
                repeat(30) {
                    compose.mainClock.advanceTimeByFrame()
                    if (compose.onAllNodesWithTag("navigation_area_Dns").fetchSemanticsNodes().isNotEmpty()) {
                        visibleFrames++
                        compose.onNodeWithText(context.getString(R.string.tab_title_dns)).assertExists()
                        compose.onNodeWithText(context.getString(R.string.tab_title_usb)).assertExists()
                    }
                }
                assertTrue("Must inspect visible closing frames", visibleFrames > 0)
            } finally {
                compose.mainClock.autoAdvance = true
            }
            compose.onNodeWithTag("navigation_area_Dns").assertDoesNotExist()
        }
    }

    @Test fun collapsingPersistentRail_shortLabelsStayCenteredDuringAnimation() {
        width = 1000
        show()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        try {
            icon(R.string.navigation_collapse).performClick()
            var inspectedFrames = 0
            repeat(40) {
                compose.mainClock.advanceTimeByFrame()
                AppArea.entries.forEach { area ->
                    val label = compose.onNodeWithTag("navigation_short_label_${area.name}", true)
                        .fetchSemanticsNode().boundsInRoot
                    val icon = compose.onNodeWithTag("navigation_icon_${area.name}", true)
                        .fetchSemanticsNode().boundsInRoot
                    // Material first fades the horizontal label out, then reveals it below the
                    // icon. At every frame in that vertical arrangement it must be centered.
                    if (label.top >= icon.bottom) {
                        inspectedFrames++
                        assertEquals("${area.name} label shifted sideways", icon.center.x, label.center.x, 1f)
                    }
                }
            }
            assertTrue("Must inspect intermediate vertical label frames", inspectedFrames > 10)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.onNodeWithTag("persistent_navigation_rail").assertWidthIsEqualTo(96.dp)
    }

}
