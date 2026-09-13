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
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppRoute

@Composable
fun UsbSettingsRoot(
    viewModel: UsbSettingsViewModel,
    onShowPermissionHelp: () -> Unit,
    route: AppRoute,
    onOpen: (AppDestination) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (!state.hasPermission) PermissionWarningCard(onShowPermissionHelp)
        when (route) {
            AppRoute.Usb -> UsbSettingsScreen(
                state,
                viewModel::onAction,
                { onOpen(AppDestination.UsbTile) },
                { onOpen(AppDestination.UsbActions) },
                { onOpen(AppDestination.UsbRevert) },
                { onOpen(AppDestination.ShortcutSettings) })

            AppRoute.UsbTile -> UsbTileScreen(state, viewModel::onAction)
            AppRoute.UsbActions -> UsbAdditionalActionsScreen(state, viewModel::onAction)
            AppRoute.UsbRevert -> UsbAutoRevertScreen(state, viewModel::onAction)
            else -> error("Not a USB destination")
        }
    }
}
