package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.feature.dns.presentation.ConfirmDeleteDialog
import com.rbn.qtsettings.feature.dns.presentation.DnsHostnameEditForm

@Composable
fun DnsDialogsRoot(viewModel: DnsSettingsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    if (editor.visible) DnsHostnameEditForm(
        editor, viewModel::editName, viewModel::editHostname,
        viewModel::dismissEditor, viewModel::saveEditor
    )
    val pendingId by viewModel.pendingDeletion.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.settings.providers.find { it.id == pendingId }?.let { provider ->
        ConfirmDeleteDialog(
            hostnameEntry = provider.toRowEntry(),
            onDismiss = { viewModel.onAction(DnsSettingsAction.Delete(null)) },
            onConfirm = viewModel::confirmDeletion
        )
    }
}
