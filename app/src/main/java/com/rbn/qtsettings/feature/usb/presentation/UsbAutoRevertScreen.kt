package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
fun UsbAutoRevertScreen(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val configurationEnabled = state.system.developerOptions || state.settings.alsoHideDevOptions
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)
    ) {
        SettingsGroup(items = listOf(0, 1), itemKey = { it }) { item ->
            if (item == 0) SettingsSwitchRow(
                stringResource(R.string.setting_enable_auto_revert),
                state.settings.enableAutoRevert,
                { onAction(UsbSettingsAction.EnableAutoRevert(it)) },
                // Every row on this page carries an icon, so their titles share one indent.
                icon = Icons.Outlined.Restore,
                enabled = configurationEnabled
            )
            else SettingsNavigationRow(
                stringResource(R.string.setting_auto_revert_delay),
                Icons.Outlined.Timer,
                { editing = true },
                status = pluralStringResource(
                    R.plurals.usb_delay_seconds,
                    state.settings.autoRevertDelaySeconds,
                    state.settings.autoRevertDelaySeconds
                ),
                enabled = configurationEnabled && state.settings.enableAutoRevert
            )
        }
        if (!configurationEnabled) Text(
            stringResource(R.string.warning_developer_options_disabled_config),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium
        )
        // The timer contract is long prose; it belongs behind a row, not around the settings.
        SettingsGroup(items = listOf(2), itemKey = { it }) {
            SettingsNavigationRow(
                stringResource(R.string.usb_revert_details),
                Icons.Outlined.Info,
                { showDetails = true }
            )
        }
    }
    if (editing) UsbRevertDelayDialog(state.settings.autoRevertDelaySeconds, { editing = false }) {
        onAction(UsbSettingsAction.AutoRevertDelaySeconds(it))
        editing = false
    }
    if (showDetails) UsbRevertDetailsDialog { showDetails = false }
}
