package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppNavigationTransitions
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.WifiIdentityAccessState

/** The editor entry owns its draft; the shared feature ViewModel commits and deletes. */
@Composable
internal fun WifiRuleEditorRoot(
    ruleId: String?,
    state: AutomationUiState,
    actions: AutomationUiActions,
    providers: List<DnsHostnameEntry>,
    onOpenLocationSettings: () -> Unit,
    onBack: () -> Unit
) {
    val rule = state.wifiNetworkRules.firstOrNull { it.id == ruleId }
    val factory = remember { viewModelFactory { initializer {
        WifiRuleEditorViewModel(createSavedStateHandle()).also { it.start(rule) }
    } } }
    val editor: WifiRuleEditorViewModel = viewModel(key = "wifi-rule-editor", factory = factory)
    val draft by editor.draft.collectAsStateWithLifecycle()
    val targetStep by editor.targetStep.collectAsStateWithLifecycle()
    val networkQuery by editor.networkQuery.collectAsStateWithLifecycle()
    val targetQuery by editor.targetQuery.collectAsStateWithLifecycle()
    val confirmDelete by editor.confirmDelete.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (state.wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE) actions.refreshWifiNetworks()
    }
    val current = draft ?: return
    // NavEntry retains content across transitions. Read the latest binding when it composes
    // so both the visible editor and the predictive preview see current draft/permission state.
    val renderStep by rememberUpdatedState<@Composable (Int) -> Unit> { step ->
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
                if (saved) onBack()
            },
            onDelete = editor::requestDelete,
            targetStep = step == 1, networkQuery = networkQuery, targetQuery = targetQuery,
            onNetworkQuery = editor::searchNetworks, onTargetQuery = editor::searchTargets,
            onNext = editor::next, onPrevious = editor::previous
        )
    }
    NavDisplay(
        backStack = if (targetStep) listOf(0, 1) else listOf(0),
        onBack = editor::previous,
        transitionSpec = { AppNavigationTransitions.forward() },
        popTransitionSpec = { AppNavigationTransitions.back() },
        predictivePopTransitionSpec = { AppNavigationTransitions.predictive() },
        entryProvider = { step ->
            NavEntry(step) {
                Surface(Modifier.fillMaxSize().testTag("wifi_rule_step_$step")) {
                    renderStep(step)
                }
            }
        }
    )
    if (confirmDelete && current.id != null) {
        AppConfirmationDialog(stringResource(R.string.wifi_rule_delete_title),
            stringResource(R.string.wifi_rule_delete_message, current.identity),
            stringResource(R.string.button_delete),
            onConfirm = { editor.cancelDelete(); actions.deleteWifiNetworkRule(current.id); onBack() },
            onDismissRequest = editor::cancelDelete, destructive = true)
    }
}
