package com.rbn.qtsettings.feature.automation.presentation

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.platform.permissions.PermissionUtils

/** Feature binding; Android permission/settings actions remain at this boundary. */
@Composable
fun AutomationRoot(
    viewModel: AutomationViewModel,
    dnsHostnames: List<DnsHostnameEntry>,
    route: AppRoute,
    onOpen: (AppDestination) -> Unit,
    onEditRule: (String) -> Unit,
    onBack: () -> Unit
) {
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
    val notificationsAllowed by viewModel.notificationsAllowed.collectAsStateWithLifecycle()
    val state = AutomationUiState(
        notificationsAllowed = notificationsAllowed,
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
    val context = LocalContext.current
    val openLocationSettings = {
        runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        Unit
    }
    when (route) {
        AppRoute.DnsWifiTarget -> AutomationDnsTargetRoot(state.dnsStateOnWifi, state.dnsHostnameOnWifi,
            dnsHostnames, { mode, host ->
                viewModel.setDnsStateOnWifi(mode); viewModel.setDnsHostnameOnWifi(host); onBack()
            })
        AppRoute.DnsMobileTarget -> AutomationDnsTargetRoot(state.dnsStateOnMobile, state.dnsHostnameOnMobile,
            dnsHostnames, { mode, host ->
                viewModel.setDnsStateOnMobile(mode); viewModel.setDnsHostnameOnMobile(host); onBack()
            })
        AppRoute.WifiRules -> WifiRulesScreen(state, dnsHostnames, viewModel::setWifiNetworkRulesEnabled,
            onAddRule = { onOpen(AppDestination.WifiRule) }, onEditRule = onEditRule,
            onRequestPermission = viewModel::launchWifiSsidPermissionRequest,
            onOpenLocationSettings = openLocationSettings,
            onOpenAppPermissionSettings = { PermissionUtils.openAppPermissionSettings(context) })
        is AppRoute.WifiRule -> WifiRuleEditorRoot(route.ruleId, state, viewModel, dnsHostnames,
            openLocationSettings, onBack)
        else -> {
            val dialogs = rememberAutomationDialogs()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                AutomationScreen(state, viewModel, dnsHostnames, { destination ->
                    when (destination) {
                        AutomationDestination.WIFI_TARGET -> onOpen(AppDestination.DnsWifiTarget)
                        AutomationDestination.MOBILE_TARGET -> onOpen(AppDestination.DnsMobileTarget)
                        else -> dialogs.open(destination)
                    }
                },
                    onNotificationAccess = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            viewModel.showOptionalNotificationExplanation()
                        } else {
                            PermissionUtils.openAppPermissionSettings(context)
                        }
                    }, onOpenRules = { onOpen(AppDestination.WifiRules) })
            }
            AutomationDialogsRoot(dialogs, state, viewModel)
        }
    }
}
