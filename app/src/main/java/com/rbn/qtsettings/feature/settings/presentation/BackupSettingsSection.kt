package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSection

@Composable
internal fun BackupSettingsSection(onExport: () -> Unit, onChooseRestore: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_backup_title)) {
        Text(stringResource(R.string.settings_backup_description))
        SettingsGroup(listOf(false, true), itemKey = { it }) { restore ->
            SettingsNavigationRow(
                title = stringResource(if (restore) R.string.backup_restore_button else R.string.backup_export_button),
                icon = if (restore) Icons.Outlined.Restore else Icons.Outlined.FileDownload,
                summary = stringResource(if (restore) R.string.backup_restore_summary else R.string.backup_export_summary),
                onClick = if (restore) onChooseRestore else onExport)
        }
    }
}
