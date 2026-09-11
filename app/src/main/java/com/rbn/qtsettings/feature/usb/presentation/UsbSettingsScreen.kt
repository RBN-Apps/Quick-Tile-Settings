package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow

/** Overview callbacks describe feature destinations; the app owns the back stack. */
@Composable
fun UsbSettingsScreen(
    state: UsbSettingsState,
    onAction: (UsbSettingsAction) -> Unit,
    onTile: () -> Unit = {},
    onAdditionalActions: () -> Unit = {},
    onRevert: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UsbStatusCard(state)
        SettingsGroup(items = listOf(0, 1, 2, 3), itemKey = { it }) { item ->
            when (item) {
                0 -> SettingsNavigationRow(
                    stringResource(R.string.usb_menu_tile), Icons.Outlined.Usb, onTile,
                    summary = stringResource(R.string.usb_menu_tile_summary)
                )

                1 -> SettingsNavigationRow(
                    stringResource(R.string.usb_menu_actions), Icons.Outlined.Settings, onAdditionalActions,
                    summary = stringResource(R.string.usb_menu_actions_summary)
                )

                2 -> SettingsNavigationRow(
                    stringResource(R.string.usb_menu_revert), Icons.Outlined.Timer, onRevert,
                    summary = stringResource(R.string.usb_menu_revert_summary),
                    status = if (state.settings.enableAutoRevert && state.settings.autoRevertDelaySeconds > 0)
                        pluralStringResource(
                            R.plurals.usb_delay_seconds,
                            state.settings.autoRevertDelaySeconds,
                            state.settings.autoRevertDelaySeconds
                        )
                    else stringResource(R.string.off_state)
                )

                3 -> SettingsSwitchRow(
                    stringResource(R.string.setting_require_unlock), state.settings.requireUnlock,
                    { onAction(UsbSettingsAction.RequireUnlock(it)) },
                    summary = stringResource(R.string.setting_require_unlock_desc), icon = Icons.Outlined.Lock
                )
            }
        }
    }
}
