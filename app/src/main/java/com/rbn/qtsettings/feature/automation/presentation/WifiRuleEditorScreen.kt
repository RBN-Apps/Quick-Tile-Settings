package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.AppSearchField
import com.rbn.qtsettings.core.designsystem.AppTextField
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity

/** Network candidates the editor can copy into the identity fields; nothing here is stored. */
data class WifiRuleNetworks(
    val current: WifiNetworkIdentity? = null,
    val nearby: List<WifiNetworkIdentity> = emptyList(),
    val recent: List<KnownWifiNetwork> = emptyList(),
    val loading: Boolean = false,
    val access: WifiIdentityAccessState = WifiIdentityAccessState.AVAILABLE
)

/** Two-step editor; the footer stays outside the scroll area and only Save commits. */
@Composable
fun WifiRuleEditorScreen(
    draft: WifiRuleDraft,
    networks: WifiRuleNetworks,
    providers: List<DnsHostnameEntry>,
    wifiDefaultLabel: String,
    onSsid: (String) -> Unit,
    onBssid: (String) -> Unit,
    onUseDetectedBssid: () -> Unit,
    onUseNetwork: (WifiNetworkIdentity) -> Unit,
    onRefreshNetworks: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onTarget: (String, String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    targetStep: Boolean = false,
    networkQuery: String = "",
    targetQuery: String = "",
    onNetworkQuery: (String) -> Unit = {},
    onTargetQuery: (String) -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val errorField = remember { BringIntoViewRequester() }
    LaunchedEffect(draft.error, targetStep) {
        if (draft.error && !targetStep) {
            withFrameNanos { }
            errorField.bringIntoView()
        }
    }
    val focus = LocalFocusManager.current
    Column(modifier.fillMaxSize().imePadding(), verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
        SupportingText(stringResource(if (targetStep) R.string.wifi_rule_step_target else R.string.wifi_rule_step_network))
        key(targetStep) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
                if (!targetStep) {
                    SettingsSection(title = stringResource(R.string.wifi_rule_network_section)) {
                        AppTextField(draft.ssid, onSsid, stringResource(R.string.wifi_ssid_label),
                            modifier = Modifier.fillMaxWidth().testTag("wifi_rule_ssid"))
                        AppTextField(draft.bssid, onBssid, stringResource(R.string.wifi_bssid_label),
                            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(errorField).testTag("wifi_rule_bssid"),
                            supportingText = stringResource(R.string.wifi_bssid_hint),
                            errorText = if (draft.error) stringResource(R.string.wifi_rule_invalid_or_duplicate) else null)
                        draft.detectedBssid?.takeIf { it != draft.bssid }?.let { detected ->
                            AppButton(stringResource(R.string.wifi_rule_use_detected_bssid, detected), onUseDetectedBssid,
                                Modifier.testTag("wifi_rule_use_detected_bssid"), style = AppButtonStyle.Text)
                        }
                    }
                    SettingsSection(title = stringResource(R.string.wifi_rule_pick_section)) {
                        SupportingText(stringResource(R.string.wifi_rule_pick_summary))
                        AppSearchField(networkQuery, onNetworkQuery, stringResource(R.string.wifi_rule_search_networks),
                            Modifier.fillMaxWidth().testTag("wifi_rule_network_search"))
                        NetworkSuggestions(networks, networkQuery, onUseNetwork, onRefreshNetworks, onRequestPermission, onOpenLocationSettings)
                    }
                } else {
                    SupportingText(draft.identity)
                    SettingsSection(title = stringResource(R.string.wifi_rule_action_label)) {
                        SupportingText(stringResource(R.string.wifi_rule_action_summary))
                        AppSearchField(targetQuery, onTargetQuery, stringResource(R.string.wifi_rule_search_targets),
                            Modifier.fillMaxWidth().testTag("wifi_rule_target_search"))
                        DnsTargetChoices(draft.action, draft.hostname, providers, targetQuery, onTarget, wifiDefaultLabel)
                    }
                }
                if (draft.id != null) {
                    AppButton(stringResource(R.string.wifi_rule_delete), onDelete,
                        Modifier.fillMaxWidth().testTag("wifi_rule_delete"), style = AppButtonStyle.Destructive,
                        icon = Icons.Outlined.Delete)
                }
            }
        }
        if (targetStep) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
                AppButton(stringResource(R.string.wifi_rule_previous), { focus.clearFocus(); onPrevious() },
                    Modifier.weight(1f).testTag("wifi_rule_previous"), style = AppButtonStyle.Secondary)
                AppButton(stringResource(R.string.dialog_save), { focus.clearFocus(); onSave() },
                    Modifier.weight(1f).testTag("wifi_rule_save"))
            }
        } else {
            AppButton(stringResource(R.string.wifi_rule_next), { focus.clearFocus(); onNext() },
                Modifier.fillMaxWidth().testTag("wifi_rule_next"))
        }
    }
}
