package com.rbn.qtsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.automation.presentation.AutomationModeDialog
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomationModeDialogInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun vpnSupportsBothModesWithOneSelectionEventPerClick() {
        var mode by mutableStateOf(TILE_ONLY_DETECTION)
        var selections = 0
        compose.setContent { QuickTileSettingsTheme {
            AutomationModeDialog(mode, vpn = true, requiresBackground = false,
                onSelect = { mode = it; selections++ }, onDismiss = {})
        } }
        compose.onNodeWithTag("vpn_detection_tile_only_option").assertIsSelected()
        compose.onNodeWithTag("vpn_detection_background_option").performScrollTo().performClick().assertIsSelected()
        assertEquals(BACKGROUND_DETECTION, mode)
        compose.onNodeWithTag("vpn_detection_tile_only_option").performScrollTo().performClick().assertIsSelected()
        assertEquals(TILE_ONLY_DETECTION, mode)
        assertEquals(2, selections)
    }

    @Test fun wifiRulesPreventSelectingTileOnlyMode() {
        var selections = 0
        compose.setContent { QuickTileSettingsTheme {
            AutomationModeDialog(BACKGROUND_DETECTION, vpn = false, requiresBackground = true,
                onSelect = { selections++ }, onDismiss = {})
        } }
        compose.onNodeWithTag("network_type_detection_tile_only_option").assertIsNotEnabled()
            .performScrollTo().performClick()
        assertEquals(0, selections)
        compose.onNodeWithTag("network_type_detection_background_option").assertIsSelected()
    }
}
