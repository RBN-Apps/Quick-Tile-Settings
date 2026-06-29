package com.rbn.qtsettings.ui.composables

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.notification_permission_dialog_title)) },
        text = { Text(stringResource(R.string.notification_permission_dialog_message)) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                openNotificationSettings(context)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.notification_permission_dialog_settings_button),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.notification_permission_dialog_settings_description)
                    )
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun NotificationPermissionExplanationDialog(
    fromBackup: Boolean,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                stringResource(
                    if (fromBackup) {
                        R.string.notification_permission_import_title
                    } else {
                        R.string.notification_permission_background_title
                    }
                )
            )
        },
        text = {
            Text(
                stringResource(
                    if (fromBackup) {
                        R.string.notification_permission_import_message
                    } else {
                        R.string.notification_permission_background_message
                    }
                )
            )
        },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text(stringResource(R.string.notification_permission_grant_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_use_tile_only_button))
            }
        }
    )
}

@Composable
fun NotificationPermissionFallbackDialog(
    onGrantPermission: () -> Unit,
    onUseTileOnly: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onUseTileOnly,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.notification_permission_fallback_title)) },
        text = { Text(stringResource(R.string.notification_permission_fallback_message)) },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text(stringResource(R.string.notification_permission_grant_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onUseTileOnly) {
                Text(stringResource(R.string.notification_permission_use_tile_only_button))
            }
        }
    )
}

@Composable
fun NotificationPermissionSettingsDialog(
    onOpenSettings: () -> Unit,
    onUseTileOnly: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onUseTileOnly,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.notification_permission_settings_title)) },
        text = { Text(stringResource(R.string.notification_permission_settings_message)) },
        confirmButton = {
            Button(onClick = {
                onOpenSettings()
                openNotificationSettings(context)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.notification_permission_settings_button),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.notification_permission_dialog_settings_description)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onUseTileOnly) {
                Text(stringResource(R.string.notification_permission_use_tile_only_button))
            }
        }
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
