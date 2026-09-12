package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ShortcutSettingsRoot(factory: ViewModelProvider.Factory, modifier: Modifier = Modifier) {
    val viewModel: ShortcutSettingsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShortcutSettingsScreen(state, viewModel::onAction, modifier)
}
