package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import androidx.compose.ui.res.pluralStringResource

@Composable
private fun wifiRulesStatus(state: AutomationUiState): String = when {
    !state.wifiNetworkRulesEnabled -> stringResource(R.string.wifi_rules_status_off)
    state.wifiNetworkRules.isEmpty() -> stringResource(R.string.wifi_rules_status_none)
    else -> pluralStringResource(R.plurals.wifi_rules_count, state.wifiNetworkRules.size, state.wifiNetworkRules.size)
}

@Composable
internal fun NetworkAutomationSection(state: AutomationUiState, actions: AutomationUiActions,
    providers: List<DnsHostnameEntry>, onOpen: (String) -> Unit, onOpenRules: () -> Unit) {
    SettingsGroup(listOf("enabled", "mode", "wifi", "rules", "mobile", "details"), { it },
        title = stringResource(R.string.automation_network_section)) { row ->
        when (row) {
            "enabled" -> SettingsSwitchRow(stringResource(R.string.setting_network_type_detection_enabled),
                state.networkTypeDetectionEnabled, actions::setNetworkTypeDetectionEnabled,
                Modifier.testTag("network_type_detection_toggle"), icon = Icons.Outlined.SwapHoriz,
                summary = stringResource(R.string.automation_network_summary))
            "mode" -> SettingsNavigationRow(stringResource(R.string.setting_network_type_detection_mode),
                Icons.Outlined.Radar, { onOpen(AutomationDialogViewModel.NETWORK_MODE) },
                Modifier.testTag("automation_network_mode"), enabled = state.networkTypeDetectionEnabled,
                status = stringResource(if (state.networkTypeDetectionMode == BACKGROUND_DETECTION)
                    R.string.automation_mode_background else R.string.automation_mode_tile))
            "wifi" -> SettingsNavigationRow(stringResource(R.string.automation_wifi), Icons.Outlined.Wifi,
                { onOpen(AutomationDialogViewModel.WIFI_TARGET) },
                Modifier.testTag("network_type_detection_wifi_state_label"),
                enabled = state.networkTypeDetectionEnabled,
                summary = if (state.wifiNetworkRulesEnabled) stringResource(R.string.automation_wifi_fallback) else null,
                status = automationTargetLabel(state.dnsStateOnWifi, state.dnsHostnameOnWifi, providers))
            "rules" -> SettingsNavigationRow(stringResource(R.string.setting_wifi_network_rules_title), Icons.Outlined.Rule,
                onOpenRules, Modifier.testTag("automation_wifi_rules"), enabled = state.networkTypeDetectionEnabled,
                status = wifiRulesStatus(state))
            "mobile" -> SettingsNavigationRow(stringResource(R.string.automation_mobile), Icons.Outlined.SignalCellularAlt,
                { onOpen(AutomationDialogViewModel.MOBILE_TARGET) },
                Modifier.testTag("network_type_detection_mobile_state_label"),
                enabled = state.networkTypeDetectionEnabled,
                status = automationTargetLabel(state.dnsStateOnMobile, state.dnsHostnameOnMobile, providers))
            "details" -> SettingsNavigationRow(stringResource(R.string.automation_network_details), Icons.Outlined.Info,
                { onOpen(AutomationDialogViewModel.NETWORK_INFO) })
        }
    }
}
