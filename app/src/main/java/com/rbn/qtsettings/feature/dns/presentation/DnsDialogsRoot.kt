package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.ui.composables.dns.DnsHostnameEditDialog
import com.rbn.qtsettings.ui.composables.dns.ConfirmDeleteDialog
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Legacy MainViewModel binding; the feature owner is extracted in #115. */
@Composable
fun DnsDialogsRoot(viewModel: MainViewModel) {
    val editingHostname by viewModel.editingHostname.collectAsStateWithLifecycle()
    if (viewModel.showHostnameEditDialog.collectAsStateWithLifecycle().value) {
        DnsHostnameEditDialog(
            entry = editingHostname,
            onDismiss = { viewModel.dismissHostnameEditDialog() },
            onSave = { id, name, hostVal ->
                if (id == null) {
                    viewModel.addCustomDnsHostname(name, hostVal)
                } else {
                    viewModel.editCustomDnsHostname(id, name, hostVal)
                }
            }
        )
    }

    val hostnamePendingDeletion by viewModel.hostnamePendingDeletion.collectAsStateWithLifecycle()
    hostnamePendingDeletion?.let { entry ->
        ConfirmDeleteDialog(
            hostnameEntry = entry,
            onDismiss = { viewModel.setHostnamePendingDeletion(null) }
        ) {
            viewModel.deleteCustomDnsHostname(entry.id)
        }
    }

}
