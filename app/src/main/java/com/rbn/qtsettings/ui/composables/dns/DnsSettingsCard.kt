package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.ui.composables.shared.CheckboxItem
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun DnsSettingsCard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val dnsToggleOff by viewModel.dnsToggleOff.collectAsState()
    val dnsToggleAuto by viewModel.dnsToggleAuto.collectAsState()
    val dnsHostnames by viewModel.dnsHostnames.collectAsState()
    val dnsListSortMode by viewModel.dnsListSortMode.collectAsState()
    val activeDnsHostname by viewModel.activeDnsHostname.collectAsState()
    val activeDnsMode by viewModel.activeDnsMode.collectAsState()
    val enableAutoRevert by viewModel.dnsEnableAutoRevert.collectAsState()
    val autoRevertDelay by viewModel.dnsAutoRevertDelaySeconds.collectAsState()
    val dnsRequireUnlock by viewModel.dnsRequireUnlock.collectAsState()
    val vpnDetectionEnabled by viewModel.vpnDetectionEnabled.collectAsState()
    val vpnDetectionMode by viewModel.vpnDetectionMode.collectAsState()
    val networkTypeDetectionEnabled by viewModel.networkTypeDetectionEnabled.collectAsState()
    val networkTypeDetectionMode by viewModel.networkTypeDetectionMode.collectAsState()
    val dnsStateOnWifi by viewModel.dnsStateOnWifi.collectAsState()
    val dnsHostnameOnWifi by viewModel.dnsHostnameOnWifi.collectAsState()
    val dnsStateOnMobile by viewModel.dnsStateOnMobile.collectAsState()
    val dnsHostnameOnMobile by viewModel.dnsHostnameOnMobile.collectAsState()
    val showDnsInfoDialogFor = remember { mutableStateOf<DnsHostnameEntry?>(null) }
    val showNetworkTypeInfoDialog = remember { mutableStateOf(false) }
    val dnsScrollState = rememberScrollState()
    var dnsScrollViewport by remember { mutableStateOf<Rect?>(null) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        val contentColor = LocalContentColor.current
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setting_title_private_dns),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.setting_desc_tile_cycles),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .onGloballyPositioned { coordinates ->
                        dnsScrollViewport = coordinates.boundsInWindow()
                    }
                    .verticalScroll(dnsScrollState)
            ) {
                DnsModeRow(
                    checked = dnsToggleOff,
                    onCheckedChange = { viewModel.setDnsToggleOff(it) },
                    label = stringResource(R.string.dns_state_off),
                    isActive = activeDnsMode == DNS_MODE_OFF,
                    onSetActiveClicked = { viewModel.setDnsMode(context, DNS_MODE_OFF) }
                )
                DnsModeRow(
                    checked = dnsToggleAuto,
                    onCheckedChange = { viewModel.setDnsToggleAuto(it) },
                    label = stringResource(R.string.dns_state_auto),
                    isActive = activeDnsMode == DNS_MODE_AUTO,
                    onSetActiveClicked = { viewModel.setDnsMode(context, DNS_MODE_AUTO) }
                )

                DnsListHeader(
                    sortMode = dnsListSortMode,
                    onSortModeSelected = viewModel::setDnsListSortMode,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                if (dnsListSortMode == DnsListSortMode.MANUAL) {
                    Text(
                        text = stringResource(R.string.dns_manual_reorder_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                DnsHostnameList(
                    entries = dnsHostnames,
                    sortMode = dnsListSortMode,
                    activeDnsHostname = activeDnsHostname,
                    scrollState = dnsScrollState,
                    scrollViewport = dnsScrollViewport,
                    onSelectionChanged = viewModel::updateDnsHostnameEntrySelection,
                    onOrderCommitted = viewModel::reorderDnsHostnames,
                    onSetActive = { entry -> viewModel.setActiveDns(context, entry) },
                    onEdit = viewModel::startEditingHostname,
                    onDelete = viewModel::setHostnamePendingDeletion,
                    onInfo = { entry -> showDnsInfoDialogFor.value = entry }
                )

                OutlinedButton(
                    onClick = { viewModel.startAddingNewHostname() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.dns_add_custom_hostname_button)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.dns_add_custom_hostname_button))
                }

                if (dnsHostnames.any { it.isSelectedForCycle && it.hostname.isBlank() }) {
                    Text(
                        text = stringResource(R.string.warning_hostname_blank),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // VPN Detection Section
                val interactionSourceVpnDetection = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSourceVpnDetection,
                            indication = null,
                            onClick = { viewModel.setVpnDetectionEnabled(!vpnDetectionEnabled) }
                        )
                ) {
                    Checkbox(
                        checked = vpnDetectionEnabled,
                        onCheckedChange = { viewModel.setVpnDetectionEnabled(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setting_vpn_detection_enabled),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (vpnDetectionEnabled) {
                    Text(
                        text = stringResource(R.string.setting_vpn_detection_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vpnDetectionMode == TILE_ONLY_DETECTION,
                                onClick = { viewModel.setVpnDetectionMode(TILE_ONLY_DETECTION) }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = vpnDetectionMode == TILE_ONLY_DETECTION,
                            onClick = { viewModel.setVpnDetectionMode(TILE_ONLY_DETECTION) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.vpn_detection_tile_only_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.vpn_detection_tile_only_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vpnDetectionMode == BACKGROUND_DETECTION,
                                onClick = { viewModel.setVpnDetectionMode(BACKGROUND_DETECTION) }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = vpnDetectionMode == BACKGROUND_DETECTION,
                            onClick = { viewModel.setVpnDetectionMode(BACKGROUND_DETECTION) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.vpn_detection_background_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.vpn_detection_background_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

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

                if (networkTypeDetectionEnabled) {
                    Text(
                        text = stringResource(R.string.setting_network_type_detection_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 12.dp)
                    )

                    // WiFi DNS State
                    Text(
                        text = stringResource(R.string.setting_dns_state_on_wifi),
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

                // Auto-Revert Section
                val interactionSourceAutoRevert = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSourceAutoRevert,
                            indication = null,
                            onClick = { viewModel.setDnsEnableAutoRevert(!enableAutoRevert) }
                        )
                ) {
                    Checkbox(
                        checked = enableAutoRevert,
                        onCheckedChange = { viewModel.setDnsEnableAutoRevert(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setting_enable_auto_revert),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_auto_revert_delay),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = contentColor.copy(
                            alpha = if (enableAutoRevert) 1f else 0.38f
                        )
                    )
                    OutlinedTextField(
                        value = autoRevertDelay.toString(),
                        onValueChange = { value ->
                            val newDelay =
                                value.toIntOrNull() ?: viewModel.dnsAutoRevertDelaySeconds.value
                            viewModel.setDnsAutoRevertDelaySeconds(newDelay)
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = enableAutoRevert
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Require Unlock Section
                CheckboxItem(
                    checked = dnsRequireUnlock,
                    onCheckedChange = { viewModel.setDnsRequireUnlock(it) },
                    label = stringResource(R.string.setting_require_unlock)
                )
                Text(
                    text = stringResource(R.string.setting_require_unlock_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                )
            }
        }
    }
    showDnsInfoDialogFor.value?.let { entry ->
        if (entry.isPredefined && entry.descriptionResId != null) {
            DnsInfoDialog(
                entry = entry,
                onDismissRequest = { showDnsInfoDialogFor.value = null }
            )
        }
    }

    if (showNetworkTypeInfoDialog.value) {
        NetworkTypeInfoDialog(
            onDismissRequest = { showNetworkTypeInfoDialog.value = false }
        )
    }
}
