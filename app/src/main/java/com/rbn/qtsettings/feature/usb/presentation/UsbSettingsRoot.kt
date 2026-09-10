package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.app.PermissionWarningCard

@Composable
fun UsbSettingsRoot(viewModel: UsbSettingsViewModel, onShowPermissionHelp: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        if (!state.hasPermission) PermissionWarningCard(onShowPermissionHelp, Modifier.padding(horizontal = 16.dp))
        Column(Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
            UsbSettingsScreen(state, viewModel::onAction)
        }
    }
}
