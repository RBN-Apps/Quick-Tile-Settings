package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionExplanationDialog
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionFallbackDialog
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionSettingsDialog
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Legacy permission-owner binding, replaced by the settings extraction in #121/#122. */
@Composable
fun NotificationDialogsRoot(viewModel: MainViewModel) {
    val showNotificationPermissionExplanationDialog by
        viewModel.showNotificationPermissionExplanationDialog.collectAsStateWithLifecycle()
    val notificationPermissionExplanationFromBackup by
        viewModel.notificationPermissionExplanationFromBackup.collectAsStateWithLifecycle()
    if (showNotificationPermissionExplanationDialog) {
        NotificationPermissionExplanationDialog(
            fromBackup = notificationPermissionExplanationFromBackup,
            onGrantPermission = { viewModel.requestNotificationPermissionFromExplanation() },
            onContinueWithoutNotifications = {
                viewModel.continueWithoutNotificationPermission()
            }
        )
    }

    val showNotificationPermissionFallbackDialog by
        viewModel.showNotificationPermissionFallbackDialog.collectAsStateWithLifecycle()
    if (showNotificationPermissionFallbackDialog) {
        NotificationPermissionFallbackDialog(
            onGrantPermission = { viewModel.requestNotificationPermissionFromExplanation() },
            onContinueWithoutNotifications = {
                viewModel.continueWithoutNotificationPermission()
            }
        )
    }

    val showNotificationPermissionSettingsDialog by
        viewModel.showNotificationPermissionSettingsDialog.collectAsStateWithLifecycle()
    if (showNotificationPermissionSettingsDialog) {
        NotificationPermissionSettingsDialog(
            onOpenSettings = { viewModel.openNotificationPermissionSettings() },
            onContinueWithoutNotifications = {
                viewModel.continueWithoutNotificationPermission()
            }
        )
    }
}
