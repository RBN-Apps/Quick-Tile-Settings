package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.WifiDnsRule

private const val ADD_ROW = "add"

/** Rule overview: one switch, the saved rules as navigation rows and the access guidance. */
@Composable
fun WifiRulesScreen(
    state: AutomationUiState,
    providers: List<DnsHostnameEntry>,
    onEnabledChange: (Boolean) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.wifiNetworkRulesEnabled && state.networkTypeDetectionEnabled
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("wifi_rules_screen"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        SettingsGroup(listOf("enabled"), { it }) {
            SettingsSwitchRow(stringResource(R.string.wifi_rules_enabled), state.wifiNetworkRulesEnabled,
                onEnabledChange, Modifier.testTag("wifi_network_rules_toggle"), icon = Icons.AutoMirrored.Outlined.Rule,
                summary = stringResource(if (state.networkTypeDetectionEnabled) R.string.automation_rules_summary
                    else R.string.automation_rules_requires_network),
                enabled = state.networkTypeDetectionEnabled)
        }
        SettingsSection(title = stringResource(R.string.wifi_rules_list_title)) {
            if (state.wifiNetworkRules.isEmpty()) SupportingText(stringResource(R.string.wifi_rules_empty_guidance))
            SettingsGroup(state.wifiNetworkRules.map { it.id } + ADD_ROW, { it }) { key ->
                if (key == ADD_ROW) {
                    SettingsNavigationRow(stringResource(R.string.wifi_rule_add), Icons.Outlined.Add, onAddRule,
                        Modifier.testTag("wifi_rule_add"), enabled = active)
                } else {
                    WifiRuleRow(state.wifiNetworkRules.first { it.id == key }, providers, active, onEditRule)
                }
            }
            if (active) SupportingText(stringResource(R.string.wifi_rules_fallback_status,
                automationTargetLabel(state.dnsStateOnWifi, state.dnsHostnameOnWifi, providers)))
        }
        if (active) WifiAccessSection(state, onRequestPermission, onOpenLocationSettings, onOpenAppPermissionSettings)
    }
}

@Composable
private fun WifiRuleRow(rule: WifiDnsRule, providers: List<DnsHostnameEntry>, enabled: Boolean,
                        onEditRule: (String) -> Unit) {
    SettingsNavigationRow(rule.ssid ?: rule.bssid.orEmpty(), Icons.Outlined.Wifi, { onEditRule(rule.id) },
        Modifier.testTag("wifi_rule_${rule.id}"), enabled = enabled,
        summary = rule.bssid?.takeIf { rule.ssid != null },
        status = automationTargetLabel(rule.actionMode, rule.dnsHostname, providers))
}

@Composable
private fun WifiAccessSection(state: AutomationUiState, onRequestPermission: () -> Unit,
                              onOpenLocationSettings: () -> Unit, onOpenAppPermissionSettings: () -> Unit) {
    val prompt = state.wifiIdentityAccessState.prompt()
    val uriHandler = LocalUriHandler.current
    val docsUrl = stringResource(R.string.wifi_ssid_permission_docs_url)
    SettingsSection(title = stringResource(R.string.wifi_rules_access_section)) {
        when {
            prompt != null -> {
                SettingsGroup(listOf("access", "docs"), { it }) { key ->
                    if (key == "access") SettingsNavigationRow(stringResource(prompt.title), Icons.Outlined.LocationOff,
                        if (prompt.opensLocationSettings) onOpenLocationSettings else onRequestPermission,
                        Modifier.testTag("wifi_identity_access_action"), summary = stringResource(prompt.description))
                    else SettingsNavigationRow(stringResource(R.string.wifi_ssid_permission_docs_link), Icons.Outlined.Info,
                        { uriHandler.openUri(docsUrl) }, Modifier.testTag("wifi_identity_access_docs"))
                }
                SupportingText(stringResource(R.string.wifi_ssid_manual_entry_notice))
            }
            !state.hasBackgroundLocationPermission -> SettingsGroup(listOf("boot"), { it }) {
                SettingsNavigationRow(stringResource(R.string.wifi_boot_title), Icons.Outlined.LocationOn,
                    onOpenAppPermissionSettings, Modifier.testTag("wifi_boot_permission_action"),
                    summary = stringResource(R.string.wifi_boot_description))
            }
            else -> SupportingText(stringResource(R.string.wifi_boot_ready))
        }
    }
}

@Composable
internal fun SupportingText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
