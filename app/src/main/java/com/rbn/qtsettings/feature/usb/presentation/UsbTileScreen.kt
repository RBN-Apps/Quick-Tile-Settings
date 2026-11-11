package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsCycleEntryRow
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
fun UsbTileScreen(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)
    ) {
        UsbTileContent(state, onAction)
    }
}

@Composable
internal fun UsbTileContent(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    val configurationEnabled = state.system.developerOptions || state.settings.alsoHideDevOptions
    val canSetActive = state.hasPermission && configurationEnabled
    SettingsSection(title = stringResource(R.string.usb_tile_cycle_title)) {
        Text(
            stringResource(R.string.usb_tile_cycle_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsGroup(items = listOf(true, false), itemKey = { it }) { enabledState ->
            val label = stringResource(if (enabledState) R.string.usb_state_on else R.string.usb_state_off)
            SettingsCycleEntryRow(
                title = label,
                checked = if (enabledState) state.settings.toggleEnable else state.settings.toggleDisable,
                onCheckedChange = {
                    onAction(if (enabledState) UsbSettingsAction.ToggleEnable(it) else UsbSettingsAction.ToggleDisable(it))
                },
                isActive = state.system.active == enabledState,
                activeLabel = stringResource(R.string.usb_state_active),
                activateLabel = stringResource(R.string.usb_set_active_mode, label),
                onActivate = { onAction(UsbSettingsAction.SetActive(enabledState)) },
                // The tile's own icons, so a state looks the same here and in Quick Settings.
                icon = ImageVector.vectorResource(if (enabledState) R.drawable.ic_usb_on else R.drawable.ic_usb_off),
                enabled = configurationEnabled,
                activateEnabled = canSetActive
            )
        }
        if (!configurationEnabled) {
            Text(
                stringResource(R.string.warning_developer_options_disabled_config),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!state.settings.toggleEnable && !state.settings.toggleDisable) {
            Text(
                stringResource(R.string.warning_at_least_one_usb_option),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
