package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog
import com.rbn.qtsettings.data.DnsHostnameEntry

@Composable
fun ConfirmDeleteDialog(
    hostnameEntry: DnsHostnameEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppConfirmationDialog(
        title = stringResource(R.string.confirm_delete_hostname_title),
        message = stringResource(R.string.confirm_delete_hostname_message, hostnameEntry.name),
        confirmLabel = stringResource(R.string.button_delete),
        onConfirm = {
            onConfirm()
            onDismiss()
        },
        onDismissRequest = onDismiss,
        destructive = true
    )
}
