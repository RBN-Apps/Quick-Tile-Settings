package com.rbn.qtsettings.ui.composables.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun AppSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportBackup(context, uri)
            }
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> pendingRestoreUri = uri }
    )

    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_backup_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_backup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppButton(
                    text = stringResource(R.string.backup_export_button),
                    icon = Icons.Outlined.FileDownload,
                    style = AppButtonStyle.Secondary,
                    onClick = { exportLauncher.launch(createBackupFilename()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    text = stringResource(R.string.backup_restore_button),
                    icon = Icons.Outlined.Restore,
                    style = AppButtonStyle.Secondary,
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val restoreUri = pendingRestoreUri
    if (restoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(text = stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.restoreBackup(context, restoreUri)
                    }
                ) {
                    Text(text = stringResource(R.string.backup_restore_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

private fun createBackupFilename(): String {
    val timestamp = LocalDateTime.now().format(BACKUP_FILENAME_TIMESTAMP_FORMAT)
    return "quick-tile-settings-backup-$timestamp.json"
}

private val BACKUP_FILENAME_TIMESTAMP_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
