package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog

@Composable
fun NotificationPermissionSettingsDialog(onOpenSettings: () -> Unit, onContinueWithoutNotifications: () -> Unit) {
    AppDialog(stringResource(R.string.notification_permission_settings_title), onContinueWithoutNotifications,
        stringResource(R.string.notification_permission_settings_button), onOpenSettings,
        dismissLabel = stringResource(R.string.notification_permission_continue_button)) {
        Text(stringResource(R.string.notification_permission_settings_message))
    }
}
