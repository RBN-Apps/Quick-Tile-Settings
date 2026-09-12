package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rbn.qtsettings.ui.composables.dns.NetworkTypeInfoDialog

@Composable
fun rememberAutomationDialogs(): AutomationDialogViewModel {
    val factory = remember { viewModelFactory { initializer { AutomationDialogViewModel(createSavedStateHandle()) } } }
    return viewModel(key = "automation-dialogs", factory = factory)
}

@Composable
internal fun AutomationDialogs(owner: AutomationDialogViewModel, state: AutomationUiState,
                               actions: AutomationUiActions) {
    val dialog by owner.dialog.collectAsStateWithLifecycle()
    when (dialog) {
        AutomationDialogViewModel.NETWORK_MODE -> AutomationModeDialog(state.networkTypeDetectionMode,
            vpn = false, requiresBackground = state.wifiNetworkRulesEnabled,
            onSelect = { owner.dismiss(); actions.setNetworkTypeDetectionMode(it) }, onDismiss = owner::dismiss)
        AutomationDialogViewModel.VPN_MODE -> AutomationModeDialog(state.vpnDetectionMode,
            vpn = true, requiresBackground = false,
            onSelect = { owner.dismiss(); actions.setVpnDetectionMode(it) }, onDismiss = owner::dismiss)
        AutomationDialogViewModel.NETWORK_INFO -> NetworkTypeInfoDialog(owner::dismiss)
    }
}
