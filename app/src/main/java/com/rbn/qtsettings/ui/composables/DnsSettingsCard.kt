package com.rbn.qtsettings.ui.composables

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun DnsSettingsCard(viewModel: MainViewModel) {
    val dnsToggleOff by viewModel.dnsToggleOff.collectAsState()
    val dnsToggleAuto by viewModel.dnsToggleAuto.collectAsState()
    val dnsHostnames by viewModel.dnsHostnames.collectAsState()
    val enableAutoRevert by viewModel.dnsEnableAutoRevert.collectAsState()
    val autoRevertDelay by viewModel.dnsAutoRevertDelaySeconds.collectAsState()
    val vpnDetectionEnabled by viewModel.vpnDetectionEnabled.collectAsState()
    val vpnDetectionMode by viewModel.vpnDetectionMode.collectAsState()
    var showDnsInfoDialogFor by remember { mutableStateOf<DnsHostnameEntry?>(null) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                    .verticalScroll(rememberScrollState())
            ) {
                CheckboxItem(
                    checked = dnsToggleOff,
                    onCheckedChange = { viewModel.setDnsToggleOff(it) },
                    label = stringResource(R.string.dns_state_off)
                )
                CheckboxItem(
                    checked = dnsToggleAuto,
                    onCheckedChange = { viewModel.setDnsToggleAuto(it) },
                    label = stringResource(R.string.dns_state_auto)
                )

                Text(
                    text = stringResource(R.string.dns_select_hostnames_for_cycle),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                dnsHostnames.forEach { entry ->
                    DnsHostnameRow(
                        entry = entry,
                        onSelectionChanged = { isSelected ->
                            viewModel.updateDnsHostnameEntrySelection(
                                entry.id,
                                isSelected
                            )
                        },
                        onEditClicked = { viewModel.startEditingHostname(entry) },
                        onDeleteClicked = { viewModel.setHostnamePendingDeletion(entry) },
                        onInfoClicked = { showDnsInfoDialogFor = entry }
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.startAddingNewHostname() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
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
                        color = if (enableAutoRevert)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
            }
        }
    }
    showDnsInfoDialogFor?.let { entry ->
        if (entry.isPredefined && entry.descriptionResId != null) {
            DnsInfoDialog(
                entry = entry,
                onDismissRequest = { showDnsInfoDialogFor = null }
            )
        }
    }
}


@Composable
fun DnsHostnameRow(
    entry: DnsHostnameEntry,
    onSelectionChanged: (Boolean) -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onInfoClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onSelectionChanged(!entry.isSelectedForCycle) }
    ) {
        Checkbox(
            checked = entry.isSelectedForCycle,
            onCheckedChange = onSelectionChanged
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (entry.isPredefined) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = entry.hostname,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (entry.isPredefined) {
            Spacer(Modifier.size(48.dp))
            if (entry.descriptionResId != null) {
                IconButton(onClick = onInfoClicked) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.button_info_dns, entry.name)
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        } else {
            IconButton(onClick = onEditClicked) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.button_edit))
            }
            IconButton(onClick = onDeleteClicked) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.button_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DnsHostnameEditDialog(
    entry: DnsHostnameEntry?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, hostname: String) -> Unit,
    viewModel: MainViewModel
) {
    var name by remember(entry) { mutableStateOf(entry?.name ?: "") }
    var hostname by remember(entry) { mutableStateOf(entry?.hostname ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var hostnameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current.applicationContext

    fun validate(): Boolean {
        nameError =
            if (name.isBlank()) context.getString(R.string.error_hostname_name_empty) else null
        hostnameError =
            if (hostname.isBlank()) context.getString(R.string.error_hostname_value_empty) else null
        return nameError == null && hostnameError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (entry == null) stringResource(R.string.dns_add_hostname_title) else stringResource(
                    R.string.dns_edit_hostname_title
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text(stringResource(R.string.dns_hostname_name_label)) },
                    isError = nameError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it; hostnameError = null },
                    label = { Text(stringResource(R.string.dns_hostname_value_label)) },
                    isError = hostnameError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hostnameError != null) {
                    Text(
                        hostnameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (validate()) {
                    onSave(entry?.id, name, hostname)
                    onDismiss()
                }
            }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    hostnameEntry: DnsHostnameEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.confirm_delete_hostname_title)) },
        text = {
            Text(
                stringResource(
                    id = R.string.confirm_delete_hostname_message,
                    hostnameEntry.name
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.button_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun DnsInfoDialog(entry: DnsHostnameEntry, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dns_info_dialog_title, entry.name)) },
        text = {
            entry.descriptionResId?.let {
                Text(stringResource(it))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}