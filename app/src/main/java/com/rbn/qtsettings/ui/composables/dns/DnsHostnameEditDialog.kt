package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppTextField
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.dns.domain.DnsEditorError
import com.rbn.qtsettings.feature.dns.domain.validateDnsEditor
import com.rbn.qtsettings.feature.dns.presentation.DnsEditorState

/** Standalone adapter used by previews and isolated dialog tests. App editing is ViewModel-owned. */
@Composable
fun DnsHostnameEditDialog(
    entry: DnsHostnameEntry?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, hostname: String) -> Unit
) {
    var name by rememberSaveable(entry?.id) { mutableStateOf(entry?.name.orEmpty()) }
    var hostname by rememberSaveable(entry?.id) { mutableStateOf(entry?.hostname.orEmpty()) }
    var errors by remember { mutableStateOf(validateDnsEditor("valid", "valid")) }
    DnsHostnameEditForm(
        DnsEditorState(true, entry?.id, name, hostname, errors.nameError, errors.hostnameError),
        { name = it; errors = errors.copy(nameError = null) },
        { hostname = it; errors = errors.copy(hostnameError = null) },
        onDismiss,
        {
            name = name.trim(); hostname = hostname.trim()
            errors = validateDnsEditor(name, hostname)
            if (errors.valid) { onSave(entry?.id, name, hostname); onDismiss() }
        }
    )
}

private fun DnsEditorError.resource(): Int = when (this) {
    DnsEditorError.NameEmpty -> R.string.error_hostname_name_empty
    DnsEditorError.HostnameEmpty -> R.string.error_hostname_value_empty
    DnsEditorError.HostnameInvalid -> R.string.error_hostname_value_invalid
}

@Composable
fun DnsHostnameEditForm(
    state: DnsEditorState,
    onNameChange: (String) -> Unit,
    onHostnameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                if (state.id == null) stringResource(R.string.dns_add_hostname_title) else stringResource(
                    R.string.dns_edit_hostname_title
                )
            )
        },
        text = {
            Column {
                AppTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.dns_hostname_name_label),
                    errorText = state.nameError?.let { stringResource(it.resource()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(
                    value = state.hostname,
                    onValueChange = onHostnameChange,
                    label = stringResource(R.string.dns_hostname_value_label),
                    errorText = state.hostnameError?.let { stringResource(it.resource()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
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
