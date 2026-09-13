package com.rbn.qtsettings

import androidx.activity.BackEventCompat
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.navigation.AppArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PermissionPagesInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun back() = compose.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
    @Before fun prepare() { compose.dismissInitialDialogsIfPresent(context) }

    @Test fun adbScrollRestores_andBothPredictiveEdgesCancelOrReturnToSetupThenUsb() {
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.clickHelpButton(context)
        node(R.string.button_show_adb_instructions).performScrollTo().performClick()
        node(R.string.button_copy_install_command).performScrollTo()
        fun offset() = compose.onNode(hasScrollAction() and hasAnyDescendant(
            hasText(context.getString(R.string.button_copy_install_command)))).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = offset()
        compose.activityRule.scenario.recreate()
        node(R.string.button_copy_install_command).assertIsDisplayed()
        assertEquals(before, offset(), 1f)
        val x = node(R.string.button_copy_install_command).fetchSemanticsNode().positionInRoot.x
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            compose.runOnIdle {
                compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, edge))
                compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, .6f, edge))
            }
            assertTrue(node(R.string.button_copy_install_command).fetchSemanticsNode().positionInRoot.x > x)
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            node(R.string.button_copy_install_command).assertIsDisplayed()
            assertEquals(before, offset(), 1f)
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        node(R.string.menu_permission_help).assertIsDisplayed()
        node(R.string.button_show_adb_instructions).assertIsDisplayed()
        back()
        node(R.string.usb_menu_tile).assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test fun optionalAutomationReturnsToSetupInUsbArea() {
        compose.selectNavigationArea(context, AppArea.Usb)
        compose.clickHelpButton(context)
        node(R.string.dns_menu_automation).performScrollTo().performClick()
        // Recreate a DNS feature destination on the USB caller's stack.
        compose.activityRule.scenario.recreate()
        back()
        node(R.string.permission_optional_summary).assertIsDisplayed()
        back()
        node(R.string.usb_menu_tile).assertIsDisplayed()
        compose.assertNavigationAreaSelected(context, AppArea.Usb)
    }
}
