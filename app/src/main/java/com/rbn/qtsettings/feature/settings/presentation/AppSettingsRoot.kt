package com.rbn.qtsettings.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsScreen
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AppSettingsRoot(
    viewModel: AppSettingsViewModel,
    onRestartOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pending by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { uri -> viewModel.export(uri.toString()) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.selectRestore(it?.toString())
    }
    AppSettingsScreen(pending,
        onExport = { export.launch("quick-tile-settings-backup-${LocalDateTime.now().format(FILENAME_FORMAT)}.json") },
        onChooseRestore = { restore.launch(arrayOf("application/json", "text/json", "*/*")) },
        onConfirmRestore = viewModel::confirmRestore,
        onCancelRestore = viewModel::cancelRestore,
        onRestartOnboarding = onRestartOnboarding,
        modifier = modifier)
}
private val FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
