package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.WifiIdentityAccessState

/**
 * One editor step per navigation entry, so a back gesture previews the whole page. The shared
 * [editor] carries the draft across both entries; the feature ViewModel commits and deletes.
 */
@Composable
internal fun WifiRuleEditorRoot(
    ruleId: String?,
    targetStep: Boolean,
    editor: WifiRuleEditorViewModel,
    state: AutomationUiState,
    actions: AutomationUiActions,
    providers: List<DnsHostnameEntry>,
    onOpenLocationSettings: () -> Unit,
    onOpenTargetStep: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val draft by editor.draft.collectAsStateWithLifecycle()
    // Opening the editor starts the session. Restoring straight into a step has no such event,
    // so seed from storage as a fallback; it is a no-op once a draft exists.
    LaunchedEffect(ruleId) {
        editor.start(state.wifiNetworkRules.firstOrNull { it.id == ruleId })
    }
    val networkQuery by editor.networkQuery.collectAsStateWithLifecycle()
    val targetQuery by editor.targetQuery.collectAsStateWithLifecycle()
    LaunchedEffect(targetStep) {
        if (!targetStep && state.wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE) {
            actions.refreshWifiNetworks()
        }
    }
    val current = draft ?: return
    WifiRuleEditorScreen(
        draft = current,
        networks = WifiRuleNetworks(state.currentWifiNetwork, state.scannedWifiNetworks, state.knownWifiNetworks,
            state.isWifiResultsLoading, state.wifiIdentityAccessState),
        providers = providers,
        wifiDefaultLabel = automationTargetLabel(state.dnsStateOnWifi, state.dnsHostnameOnWifi, providers),
        onSsid = editor::ssid, onBssid = editor::bssid, onUseDetectedBssid = editor::useDetectedBssid,
        onUseNetwork = editor::useNetwork, onRefreshNetworks = actions::refreshWifiNetworks,
        onRequestPermission = actions::launchWifiSsidPermissionRequest,
        onOpenLocationSettings = onOpenLocationSettings, onTarget = editor::target,
        onSave = {
            val saved = editor.save { id, ssid, bssid, action, host ->
                if (id == null) actions.addWifiNetworkRule(ssid, bssid, action, host)
                else actions.updateWifiNetworkRule(id, ssid, bssid, action, host)
            }
            // A rejected rule is an identity problem, and the network step owns that error.
            if (saved) onClose() else onBack()
        },
        onDelete = editor::requestDelete,
        targetStep = targetStep, networkQuery = networkQuery, targetQuery = targetQuery,
        onNetworkQuery = editor::searchNetworks, onTargetQuery = editor::searchTargets,
        onNext = { if (editor.validateNetwork()) onOpenTargetStep() },
        onPrevious = onBack
    )
}
