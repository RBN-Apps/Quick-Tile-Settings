package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.DnsHostnameValidator

@Composable
fun DnsHostnameEditDialog(
    entry: DnsHostnameEntry?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, hostname: String) -> Unit
) {
    var name by remember(entry) { mutableStateOf(entry?.name ?: "") }
    var hostname by remember(entry) { mutableStateOf(entry?.hostname ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var hostnameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current.applicationContext

    fun validate(): Boolean {
        name = name.trim()
        hostname = hostname.trim()
        nameError =
            if (name.isBlank()) context.getString(R.string.error_hostname_name_empty) else null
        hostnameError = when {
            hostname.isBlank() -> context.getString(R.string.error_hostname_value_empty)
            !DnsHostnameValidator.isValid(hostname) ->
                context.getString(R.string.error_hostname_value_invalid)

            else -> null
        }
        return nameError == null && hostnameError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
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

@Composable
fun DnsStateSelector(
    dnsState: String,
    dnsHostname: String?,
    dnsHostnames: List<DnsHostnameEntry>,
    onDnsStateChange: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = when (dnsState) {
                    DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
                    DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
                    DNS_MODE_ON -> {
                        val entry = dnsHostnames.find { it.hostname == dnsHostname }
                        entry?.name ?: dnsHostname
                        ?: stringResource(R.string.setting_select_hostname)
                    }

                    else -> stringResource(R.string.dns_mode_auto_label)
                },
                modifier = Modifier.weight(1f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dns_mode_off_label)) },
                onClick = {
                    onDnsStateChange(DNS_MODE_OFF, null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dns_mode_auto_label)) },
                onClick = {
                    onDnsStateChange(DNS_MODE_AUTO, null)
                    expanded = false
                }
            )
            HorizontalDivider()
            dnsHostnames.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(entry.name)
                            Text(
                                text = entry.hostname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onDnsStateChange(DNS_MODE_ON, entry.hostname)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NetworkTypeInfoDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.network_type_info_title)) },
        text = {
            Text(stringResource(R.string.network_type_info_message))
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}
