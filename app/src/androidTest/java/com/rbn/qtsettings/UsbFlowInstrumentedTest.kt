package com.rbn.qtsettings

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class UsbFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun usbDebuggingSettings_shouldShowAllOptions() {
        // Navigate to USB area
        composeTestRule.navigateToUsbArea(context)

        // Verify USB debugging settings title
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_tile)).performClick()

        // Verify tile cycles description
        composeTestRule.onNodeWithText(context.getString(R.string.usb_tile_cycle_explanation))
            .assertIsDisplayed()

        // Verify USB state options exist
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_on))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_off))

        Log.i(FLOW_TAG, "USB debugging settings displayed correctly")
    }

    @Test
    fun usbOnAndOffRows_shouldOfferSetActiveAction() {
        composeTestRule.navigateToUsbArea(context)
        composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_tile)).performClick()

        listOf(
            context.getString(R.string.usb_state_on),
            context.getString(R.string.usb_state_off)
        ).forEach { stateLabel ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.usb_set_active_mode, stateLabel)
            )
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun usbSettings_shouldHandleCheckboxInteractions() {
        // Navigate to USB area
        composeTestRule.navigateToUsbArea(context)
        composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_tile)).performClick()

        Log.i(FLOW_TAG, "Testing USB checkbox interactions")

        // Test USB Enable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_on))
            .performClick()
        composeTestRule.waitForIdle()

        // Test USB Disable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_off))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_revert)).performClick()
        // Test USB Auto-Revert checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
            .performClick()
        composeTestRule.waitForIdle()
    }
}
