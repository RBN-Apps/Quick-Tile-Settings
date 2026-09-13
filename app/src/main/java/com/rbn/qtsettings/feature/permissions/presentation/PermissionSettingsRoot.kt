package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.feature.permissions.domain.GrantMethod

@Composable
fun PermissionSettingsRoot(
    viewModel: AppPermissionViewModel,
    onOpenAdb: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onOpenAutomation: () -> Unit
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val ready by viewModel.initialized.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(Unit) { viewModel.refresh() }
    // Preserve one-time help, including system/predictive Back and rail exits. #150 owns onboarding.
    LaunchedEffect(ready) {
        if (ready && !snapshot.hasWriteSecureSettings) viewModel.setHelpShown(true)
    }
    PermissionSettingsScreen(snapshot, ready, progress,
        feedback = progress.feedback?.resolve(resources),
        onRefresh = viewModel::refresh, onOpenAdb = onOpenAdb,
        onOpenDeveloperOptions = onOpenDeveloperOptions,
        onRequestShizukuPermission = onRequestShizukuPermission,
        onGrantShizuku = { viewModel.grant(GrantMethod.Shizuku) },
        onGrantRoot = { viewModel.grant(GrantMethod.Root) },
        onOpenAutomation = onOpenAutomation)
}
