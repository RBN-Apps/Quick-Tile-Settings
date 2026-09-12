package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity

@Composable
fun WifiNetworkRulesSection(
    enabled: Boolean,
    rules: List<WifiDnsRule>,
    dnsHostnames: List<DnsHostnameEntry>,
    currentNetwork: WifiNetworkIdentity?,
    scannedNetworks: List<WifiNetworkIdentity>,
    knownNetworks: List<KnownWifiNetwork>,
    isLoadingNetworks: Boolean,
    wifiIdentityAccessState: WifiIdentityAccessState,
    onEnabledChange: (Boolean) -> Unit,
    onAddRule: (ssid: String?, bssid: String?, action: String, hostname: String?) -> Boolean,
    onUpdateRule: (id: String, ssid: String?, bssid: String?, action: String, hostname: String?) -> Boolean,
    onDeleteRule: (String) -> Unit,
    onRefreshNetworks: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    hasBackgroundLocationPermission: Boolean,
    onOpenAppPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editorSeed by rememberSaveable(stateSaver = WifiRuleEditorSeed.NullableSaver) {
        mutableStateOf(null)
    }
    var rulePendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNetworkPicker by rememberSaveable { mutableStateOf(false) }
    val rulePendingDeletion = rules.firstOrNull { it.id == rulePendingDeletionId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wifi_network_rules_section")
    ) {
        Text(
            text = stringResource(R.string.setting_wifi_network_rules_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.setting_wifi_network_rules_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wifi_network_rules_toggle")
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .padding(vertical = 4.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.wifi_rules_enabled),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.wifi_rules_enabled_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = null)
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(12.dp))
            if (wifiIdentityAccessState != WifiIdentityAccessState.AVAILABLE) {
                WifiIdentityAccessCard(
                    accessState = wifiIdentityAccessState,
                    onRequestPermission = onRequestPermission,
                    onOpenLocationSettings = onOpenLocationSettings
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE) {
                if (hasBackgroundLocationPermission) {
                    Text(
                        stringResource(R.string.wifi_boot_ready),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.wifi_boot_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.wifi_boot_description), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = onOpenAppPermissionSettings,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("wifi_boot_permission_action")
                            ) { Text(stringResource(R.string.wifi_permission_open_settings)) }
                        }
                    }
                }
            }

            Text(text = stringResource(R.string.wifi_rules_title), style = MaterialTheme.typography.titleSmall)
            if (rules.isEmpty()) {
                Text(
                    text = stringResource(R.string.wifi_rules_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    rules.forEachIndexed { index, rule ->
                        WifiRuleRow(
                            rule = rule,
                            dnsHostnames = dnsHostnames,
                            onEdit = { editorSeed = WifiRuleEditorSeed.fromRule(rule) },
                            onDelete = { rulePendingDeletionId = rule.id }
                        )
                        if (index < rules.lastIndex) HorizontalDivider()
                    }
                }
            }

            currentNetwork?.let { identity ->
                OutlinedButton(
                    onClick = { editorSeed = WifiRuleEditorSeed.fromIdentity(identity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("wifi_rule_add_current")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.wifi_rule_add_current))
                }
            }

            OutlinedButton(
                onClick = { showNetworkPicker = true; onRefreshNetworks() },
                enabled = wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("wifi_rule_choose_network")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.wifi_rule_choose_network))
            }

            OutlinedButton(
                onClick = { editorSeed = WifiRuleEditorSeed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("wifi_rule_add_manual")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.wifi_rule_add_manual))
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            editorSeed = null
            rulePendingDeletionId = null
            showNetworkPicker = false
        }
    }

    if (enabled && showNetworkPicker) {
        WifiNetworkPickerDialog(
            currentNetwork = currentNetwork,
            scannedNetworks = scannedNetworks,
            knownNetworks = knownNetworks,
            isLoadingNetworks = isLoadingNetworks,
            onRefresh = onRefreshNetworks,
            onSelect = { identity ->
                showNetworkPicker = false
                editorSeed = WifiRuleEditorSeed.fromIdentity(identity)
            },
            onDismiss = { showNetworkPicker = false }
        )
    }

    if (enabled) editorSeed?.let { seed ->
        WifiRuleEditDialog(
            seed = seed,
            dnsHostnames = dnsHostnames,
            onDismiss = { editorSeed = null },
            onSave = { id, ssid, bssid, action, hostname ->
                val saved = if (id == null) {
                    onAddRule(ssid, bssid, action, hostname)
                } else {
                    onUpdateRule(id, ssid, bssid, action, hostname)
                }
                if (saved) editorSeed = null
                saved
            }
        )
    }

    rulePendingDeletion?.let { rule ->
        AlertDialog(
            onDismissRequest = { rulePendingDeletionId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(stringResource(R.string.wifi_rule_delete_title)) },
            text = { Text(stringResource(R.string.wifi_rule_delete_message, rule.displayIdentity())) },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteRule(rule.id); rulePendingDeletionId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { rulePendingDeletionId = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
