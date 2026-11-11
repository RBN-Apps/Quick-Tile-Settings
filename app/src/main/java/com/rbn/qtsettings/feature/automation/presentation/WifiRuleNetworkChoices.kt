package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.WifiNetworkIdentity

internal enum class CandidateKind { CURRENT, NEARBY, RECENT }
internal data class Candidate(val identity: WifiNetworkIdentity, val kind: CandidateKind)

@Composable
internal fun NetworkSuggestions(
    networks: WifiRuleNetworks,
    query: String,
    onUseNetwork: (WifiNetworkIdentity) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val prompt = networks.access.prompt()
    val candidates = buildList {
        networks.current?.let { add(Candidate(it, CandidateKind.CURRENT)) }
        networks.nearby.forEach { add(Candidate(it, CandidateKind.NEARBY)) }
        networks.recent.forEach { add(Candidate(it.identity, CandidateKind.RECENT)) }
    }.distinctBy { it.identity.stableKey }.filter {
        query.isBlank() || it.identity.ssid.orEmpty().contains(query.trim(), ignoreCase = true) ||
            it.identity.bssid.orEmpty().contains(query.trim(), ignoreCase = true)
    }
    if (prompt != null) {
        SupportingText(stringResource(R.string.automation_picker_access))
        AppButton(stringResource(prompt.action),
            if (prompt.opensLocationSettings) onOpenLocationSettings else onRequestPermission,
            Modifier.testTag("wifi_identity_access_action"), style = AppButtonStyle.Secondary)
    }
    if (networks.loading) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.wifi_network_scanning))
        }
    } else if (candidates.isEmpty()) {
        SupportingText(stringResource(if (query.isNotBlank()) R.string.wifi_rule_search_empty else if (prompt == null) R.string.wifi_network_none_found else R.string.automation_known_empty))
    }
    SettingsGroup(candidates, { it.identity.stableKey }) { candidate ->
        SettingsNavigationRow(
            candidate.identity.ssid ?: stringResource(R.string.wifi_hidden_network),
            if (candidate.kind == CandidateKind.RECENT) Icons.Outlined.History else Icons.Outlined.Wifi,
            { onUseNetwork(candidate.identity) },
            summary = stringResource(when (candidate.kind) {
                CandidateKind.CURRENT -> R.string.wifi_network_current_title
                CandidateKind.NEARBY -> R.string.wifi_network_nearby_title
                CandidateKind.RECENT -> R.string.wifi_network_history_title
            }),
            status = candidate.identity.bssid
        )
    }
    AppButton(stringResource(R.string.wifi_network_refresh), onRefresh, Modifier.testTag("wifi_network_refresh"),
        style = AppButtonStyle.Text, enabled = !networks.loading && prompt == null, icon = Icons.Outlined.Refresh)
}

