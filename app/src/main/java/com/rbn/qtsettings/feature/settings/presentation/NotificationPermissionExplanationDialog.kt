package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog

@Composable
fun NotificationPermissionExplanationDialog(fromBackup: Boolean, onGrantPermission: () -> Unit,
    onContinueWithoutNotifications: () -> Unit) {
    AppDialog(stringResource(if (fromBackup) R.string.notification_permission_import_title
        else R.string.notification_permission_background_title), onContinueWithoutNotifications,
        stringResource(R.string.notification_permission_grant_button), onGrantPermission,
        dismissLabel = stringResource(R.string.notification_permission_continue_button)) {
        Text(stringResource(if (fromBackup) R.string.notification_permission_import_message
            else R.string.notification_permission_background_message))
    }
}
