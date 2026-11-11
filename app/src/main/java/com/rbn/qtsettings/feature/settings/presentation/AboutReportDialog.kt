package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.SettingsCheckboxRow

@Composable
fun AboutReportDialog(
    details: String,
    includeDetails: Boolean,
    onInclude: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(stringResource(R.string.about_report_issue), onDismiss,
        stringResource(R.string.about_report_open), onOpen,
        dismissLabel = stringResource(R.string.dialog_cancel)) {
        Text(stringResource(R.string.about_report_explanation))
        SettingsCheckboxRow(stringResource(R.string.about_report_include), includeDetails, onInclude,
            summary = stringResource(R.string.about_report_privacy))
        if (includeDetails) Text(details, style = MaterialTheme.typography.bodySmall)
    }
}
