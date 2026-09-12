package com.rbn.qtsettings.feature.settings.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionExplanationDialog
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionFallbackDialog
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionSettingsDialog

/** One notification dialog at a time; global help takes precedence without discarding pending state. */
@Composable
fun NotificationDialogsRoot(viewModel: AutomationViewModel, visible: Boolean = true) {
    val explanation by viewModel.showNotificationPermissionExplanationDialog.collectAsStateWithLifecycle()
    val fromBackup by viewModel.notificationPermissionExplanationFromBackup.collectAsStateWithLifecycle()
    val fallback by viewModel.showNotificationPermissionFallbackDialog.collectAsStateWithLifecycle()
    val settings by viewModel.showNotificationPermissionSettingsDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (!visible) return
    when {
        settings -> NotificationPermissionSettingsDialog(
            onOpenSettings = {
                viewModel.openNotificationPermissionSettings()
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }, onContinueWithoutNotifications = viewModel::continueWithoutNotificationPermission)
        fallback -> NotificationPermissionFallbackDialog(viewModel::requestNotificationPermissionFromExplanation,
            viewModel::continueWithoutNotificationPermission)
        explanation -> NotificationPermissionExplanationDialog(fromBackup, viewModel::requestNotificationPermissionFromExplanation,
            viewModel::continueWithoutNotificationPermission)
    }
}
