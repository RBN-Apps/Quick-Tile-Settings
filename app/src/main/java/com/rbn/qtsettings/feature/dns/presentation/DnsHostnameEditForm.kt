package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.AppTextField
import com.rbn.qtsettings.feature.dns.domain.DnsEditorError
import com.rbn.qtsettings.feature.dns.presentation.DnsEditorState

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
    AppDialog(
        title = stringResource(if (state.id == null) R.string.dns_add_hostname_title else R.string.dns_edit_hostname_title),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.dialog_save), onConfirm = onSave,
        dismissLabel = stringResource(R.string.dialog_cancel),
        properties = DialogProperties(decorFitsSystemWindows = true)
    ) {
        AppTextField(
            value = state.name, onValueChange = onNameChange,
            label = stringResource(R.string.dns_hostname_name_label),
            errorText = state.nameError?.let { stringResource(it.resource()) },
            modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            value = state.hostname, onValueChange = onHostnameChange,
            label = stringResource(R.string.dns_hostname_value_label),
            errorText = state.hostnameError?.let { stringResource(it.resource()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
