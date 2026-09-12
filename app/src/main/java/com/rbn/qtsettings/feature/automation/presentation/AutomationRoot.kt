package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.data.DnsHostnameEntry

/** Feature-owned automation binding. Visual redesign follows in #118. */
@Composable
fun AutomationRoot(viewModel: AutomationViewModel, dnsHostnames: List<DnsHostnameEntry>) {
    val vpnDetectionEnabled by viewModel.vpnDetectionEnabled.collectAsStateWithLifecycle()
    val vpnDetectionMode by viewModel.vpnDetectionMode.collectAsStateWithLifecycle()
    val networkTypeDetectionEnabled by viewModel.networkTypeDetectionEnabled.collectAsStateWithLifecycle()
    val networkTypeDetectionMode by viewModel.networkTypeDetectionMode.collectAsStateWithLifecycle()
    val dnsStateOnWifi by viewModel.dnsStateOnWifi.collectAsStateWithLifecycle()
    val dnsHostnameOnWifi by viewModel.dnsHostnameOnWifi.collectAsStateWithLifecycle()
    val wifiNetworkRulesEnabled by viewModel.wifiNetworkRulesEnabled.collectAsStateWithLifecycle()
    val wifiNetworkRules by viewModel.wifiNetworkRules.collectAsStateWithLifecycle()
    val knownWifiNetworks by viewModel.knownWifiNetworks.collectAsStateWithLifecycle()
    val currentWifiNetwork by viewModel.currentWifiNetwork.collectAsStateWithLifecycle()
    val scannedWifiNetworks by viewModel.scannedWifiNetworks.collectAsStateWithLifecycle()
    val isWifiResultsLoading by viewModel.isWifiResultsLoading.collectAsStateWithLifecycle()
    val wifiIdentityAccessState by viewModel.wifiIdentityAccessState.collectAsStateWithLifecycle()
    val hasBackgroundLocationPermission by viewModel.hasBackgroundLocationPermission.collectAsStateWithLifecycle()
    val dnsStateOnMobile by viewModel.dnsStateOnMobile.collectAsStateWithLifecycle()
    val dnsHostnameOnMobile by viewModel.dnsHostnameOnMobile.collectAsStateWithLifecycle()
    val state = AutomationUiState(
        vpnDetectionEnabled = vpnDetectionEnabled,
        vpnDetectionMode = vpnDetectionMode,
        networkTypeDetectionEnabled = networkTypeDetectionEnabled,
        networkTypeDetectionMode = networkTypeDetectionMode,
        dnsStateOnWifi = dnsStateOnWifi,
        dnsHostnameOnWifi = dnsHostnameOnWifi,
        wifiNetworkRulesEnabled = wifiNetworkRulesEnabled,
        wifiNetworkRules = wifiNetworkRules,
        knownWifiNetworks = knownWifiNetworks,
        currentWifiNetwork = currentWifiNetwork,
        scannedWifiNetworks = scannedWifiNetworks,
        isWifiResultsLoading = isWifiResultsLoading,
        wifiIdentityAccessState = wifiIdentityAccessState,
        hasBackgroundLocationPermission = hasBackgroundLocationPermission,
        dnsStateOnMobile = dnsStateOnMobile,
        dnsHostnameOnMobile = dnsHostnameOnMobile
    )
    VpnAutomationSection(state, viewModel)
    NetworkAutomationSection(state, viewModel, dnsHostnames)
}
