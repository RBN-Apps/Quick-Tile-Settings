package com.rbn.qtsettings.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsWarningCard

@Composable
fun PermissionWarningCard(onGrantPermissionClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsWarningCard(stringResource(R.string.warning_permission_missing_title),
        stringResource(R.string.warning_permission_missing_desc), stringResource(R.string.button_show_permission_help),
        onGrantPermissionClick, modifier)
}
