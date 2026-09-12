package com.rbn.qtsettings.feature.automation.presentation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.ui.composables.WifiNetworkRulesSection
import com.rbn.qtsettings.ui.composables.dns.DnsStateSelector
import com.rbn.qtsettings.ui.composables.dns.NetworkTypeInfoDialog
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.PermissionUtils

@Composable
internal fun NetworkAutomationSection(state: AutomationUiState, viewModel: AutomationUiActions, dnsHostnames: List<DnsHostnameEntry>) {
    val networkTypeDetectionEnabled = state.networkTypeDetectionEnabled
    val networkTypeDetectionMode = state.networkTypeDetectionMode
    val dnsStateOnWifi = state.dnsStateOnWifi
    val dnsHostnameOnWifi = state.dnsHostnameOnWifi
    val wifiNetworkRulesEnabled = state.wifiNetworkRulesEnabled
    val wifiNetworkRules = state.wifiNetworkRules
    val knownWifiNetworks = state.knownWifiNetworks
    val currentWifiNetwork = state.currentWifiNetwork
    val scannedWifiNetworks = state.scannedWifiNetworks
    val isWifiResultsLoading = state.isWifiResultsLoading
    val wifiIdentityAccessState = state.wifiIdentityAccessState
    val hasBackgroundLocationPermission = state.hasBackgroundLocationPermission
    val dnsStateOnMobile = state.dnsStateOnMobile
    val dnsHostnameOnMobile = state.dnsHostnameOnMobile
    val context = LocalContext.current
    val showNetworkTypeInfoDialog = remember { mutableStateOf(false) }
    // Network Type Detection Section
    val interactionSourceNetworkDetection = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .testTag("network_type_detection_toggle")
                .clickable(
                    interactionSource = interactionSourceNetworkDetection,
                    indication = null,
                    onClick = { viewModel.setNetworkTypeDetectionEnabled(!networkTypeDetectionEnabled) }
                )
        ) {
            Checkbox(
                checked = networkTypeDetectionEnabled,
                onCheckedChange = { viewModel.setNetworkTypeDetectionEnabled(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.setting_network_type_detection_enabled),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        IconButton(onClick = { showNetworkTypeInfoDialog.value = true }) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.network_type_info_title)
            )
        }
    }

    Text(
        text = stringResource(R.string.setting_network_type_detection_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 12.dp)
    )

    if (networkTypeDetectionEnabled) {
        // WiFi DNS State
        Text(
            text = stringResource(
                if (wifiNetworkRulesEnabled) {
                    R.string.setting_dns_state_on_wifi_other_networks
                } else {
                    R.string.setting_dns_state_on_wifi
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .testTag("network_type_detection_wifi_state_label")
        )
        DnsStateSelector(
            dnsState = dnsStateOnWifi,
            dnsHostname = dnsHostnameOnWifi,
            dnsHostnames = dnsHostnames,
            onDnsStateChange = { state, hostname ->
                viewModel.setDnsStateOnWifi(state)
                if (state == DNS_MODE_ON) {
                    viewModel.setDnsHostnameOnWifi(hostname)
                } else {
                    viewModel.setDnsHostnameOnWifi(null)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        WifiNetworkRulesSection(
            enabled = wifiNetworkRulesEnabled,
            rules = wifiNetworkRules,
            dnsHostnames = dnsHostnames,
            currentNetwork = currentWifiNetwork,
            scannedNetworks = scannedWifiNetworks,
            knownNetworks = knownWifiNetworks,
            isLoadingNetworks = isWifiResultsLoading,
            wifiIdentityAccessState = wifiIdentityAccessState,
            onEnabledChange = viewModel::setWifiNetworkRulesEnabled,
            onAddRule = viewModel::addWifiNetworkRule,
            onUpdateRule = viewModel::updateWifiNetworkRule,
            onDeleteRule = viewModel::deleteWifiNetworkRule,
            onRefreshNetworks = viewModel::refreshWifiNetworks,
            onRequestPermission = viewModel::launchWifiSsidPermissionRequest,
            onOpenLocationSettings = { openLocationSettings(context) },
            hasBackgroundLocationPermission = hasBackgroundLocationPermission,
            onOpenAppPermissionSettings = { PermissionUtils.openAppPermissionSettings(context) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mobile Data DNS State
        Text(
            text = stringResource(R.string.setting_dns_state_on_mobile),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .testTag("network_type_detection_mobile_state_label")
        )
        DnsStateSelector(
            dnsState = dnsStateOnMobile,
            dnsHostname = dnsHostnameOnMobile,
            dnsHostnames = dnsHostnames,
            onDnsStateChange = { state, hostname ->
                viewModel.setDnsStateOnMobile(state)
                if (state == DNS_MODE_ON) {
                    viewModel.setDnsHostnameOnMobile(hostname)
                } else {
                    viewModel.setDnsHostnameOnMobile(null)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Detection Mode Selection
        val tileOnlyDetectionAvailable = !wifiNetworkRulesEnabled
        Text(
            text = stringResource(R.string.setting_network_type_detection_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("network_type_detection_tile_only_option")
                .selectable(
                    selected = networkTypeDetectionMode == TILE_ONLY_DETECTION,
                    enabled = tileOnlyDetectionAvailable,
                    onClick = {
                        viewModel.setNetworkTypeDetectionMode(
                            TILE_ONLY_DETECTION
                        )
                    }
                )
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = networkTypeDetectionMode == TILE_ONLY_DETECTION,
                enabled = tileOnlyDetectionAvailable,
                onClick = { viewModel.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.network_type_detection_tile_only_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.network_type_detection_tile_only_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!tileOnlyDetectionAvailable) {
                    Text(
                        text = stringResource(
                            R.string.wifi_rules_require_background_detection
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("network_type_detection_background_option")
                .selectable(
                    selected = networkTypeDetectionMode == BACKGROUND_DETECTION,
                    onClick = {
                        viewModel.setNetworkTypeDetectionMode(
                            BACKGROUND_DETECTION
                        )
                    }
                )
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = networkTypeDetectionMode == BACKGROUND_DETECTION,
                onClick = { viewModel.setNetworkTypeDetectionMode(BACKGROUND_DETECTION) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.network_type_detection_background_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.network_type_detection_background_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    if (showNetworkTypeInfoDialog.value) {
        NetworkTypeInfoDialog(onDismissRequest = { showNetworkTypeInfoDialog.value = false })
    }
}

private fun openLocationSettings(context: Context) {
    val locationSettings = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    runCatching { context.startActivity(locationSettings) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}
