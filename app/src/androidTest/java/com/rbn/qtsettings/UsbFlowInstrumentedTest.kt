package com.rbn.qtsettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.app.AppStores
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The installed app wires the USB pages to the real settings store. Screen behaviour is covered by
 * [UsbPilotInstrumentedTest] and the store by UsbSettingsViewModelTest; only the wiring is left.
 */
@RunWith(AndroidJUnit4::class)
class UsbFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun tileAndRevertPages_offerBothStates_andPersistTheirSelections() {
        val store = AppStores.getInstance(context).usb
        val original = store.state.value
        try {
            composeTestRule.navigateToUsbArea(context)
            composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_tile)).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.usb_tile_cycle_explanation))
                .assertIsDisplayed()
            listOf(R.string.usb_state_on, R.string.usb_state_off).forEach { state ->
                composeTestRule.onNodeWithContentDescription(
                    context.getString(R.string.usb_set_active_mode, context.getString(state))
                ).performScrollTo().assertIsDisplayed()
            }

            composeTestRule.onNodeWithText(context.getString(R.string.usb_state_on))
                .performScrollTo().performClick()
            composeTestRule.runOnIdle {
                assertEquals(!original.toggleEnable, store.state.value.toggleEnable)
            }

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_revert))
                .performScrollTo().performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
                .performClick()
            composeTestRule.runOnIdle {
                assertEquals(!original.enableAutoRevert, store.state.value.enableAutoRevert)
            }
        } finally {
            store.setToggleEnable(original.toggleEnable)
            store.setEnableAutoRevert(original.enableAutoRevert)
        }
    }
}
