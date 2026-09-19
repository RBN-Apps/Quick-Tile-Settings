package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** The device-dependent action limit is named in the explanation, so it comes from the same owner. */
@Composable
fun ShortcutHelpRoot(
    factory: ViewModelProvider.Factory,
    onPermissionHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ShortcutSettingsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShortcutHelpScreen(state.configuration.maxCount, onPermissionHelp, modifier)
}
