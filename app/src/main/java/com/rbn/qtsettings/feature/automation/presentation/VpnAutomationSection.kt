package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION

@Composable
internal fun VpnAutomationSection(state: AutomationUiState, actions: AutomationUiActions,
                                  onOpen: (String) -> Unit) {
    SettingsGroup(listOf("enabled", "mode"), { it }, title = stringResource(R.string.automation_vpn_section)) { row ->
        if (row == "enabled") SettingsSwitchRow(stringResource(R.string.setting_vpn_detection_enabled),
            state.vpnDetectionEnabled, actions::setVpnDetectionEnabled,
            Modifier.testTag("vpn_detection_toggle"), icon = Icons.Outlined.VpnKey,
            summary = stringResource(R.string.automation_vpn_summary))
        else SettingsNavigationRow(stringResource(R.string.setting_vpn_detection_mode), Icons.Outlined.Radar,
            { onOpen(AutomationDestination.VPN_MODE) }, Modifier.testTag("automation_vpn_mode"),
            enabled = state.vpnDetectionEnabled,
            status = stringResource(if (state.vpnDetectionMode == BACKGROUND_DETECTION)
                R.string.automation_mode_background else R.string.automation_mode_tile))
    }
}
