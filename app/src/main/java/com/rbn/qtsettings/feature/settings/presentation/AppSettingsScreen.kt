package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.settings.presentation.BackupSettingsSection

@Composable
fun AppSettingsScreen(
    pendingRestore: String?,
    onExport: () -> Unit,
    onChooseRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onRestartOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = AppSpacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        BackupSettingsSection(onExport, onChooseRestore)
        SettingsGroup(listOf(Unit), itemKey = { "onboarding" }) {
            SettingsNavigationRow(stringResource(R.string.onboarding_replay_title),
                Icons.Outlined.Explore, onRestartOnboarding,
                summary = stringResource(R.string.onboarding_replay_summary))
        }
    }
    if (pendingRestore != null) {
        AppConfirmationDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            message = stringResource(R.string.backup_restore_confirm_message),
            confirmLabel = stringResource(R.string.backup_restore_confirm_button),
            onConfirm = onConfirmRestore, onDismissRequest = onCancelRestore)
    }
}
