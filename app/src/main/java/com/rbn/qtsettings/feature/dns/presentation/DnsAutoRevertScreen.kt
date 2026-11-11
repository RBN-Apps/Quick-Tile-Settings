package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
fun DnsAutoRevertScreen(state: DnsSettingsState, onAction: (DnsSettingsAction) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        SettingsGroup(items = listOf(0, 1), itemKey = { it }) { item ->
            if (item == 0) SettingsSwitchRow(stringResource(R.string.setting_enable_auto_revert), state.settings.enableAutoRevert,
                { onAction(DnsSettingsAction.EnableAutoRevert(it)) }, icon = Icons.Outlined.Restore)
            else SettingsNavigationRow(stringResource(R.string.setting_auto_revert_delay), Icons.Outlined.Timer, { editing = true },
                status = pluralStringResource(R.plurals.usb_delay_seconds, state.settings.autoRevertDelaySeconds, state.settings.autoRevertDelaySeconds),
                enabled = state.settings.enableAutoRevert)
        }
        SettingsGroup(items = listOf(0), itemKey = { it }) {
            SettingsNavigationRow(stringResource(R.string.usb_revert_details), Icons.Outlined.Info, { details = true })
        }
    }
    if (editing) RevertDelayDialog(state.settings.autoRevertDelaySeconds, { editing = false },
        positiveOnly = false, invalidValueMessage = stringResource(R.string.dns_delay_error)) {
        onAction(DnsSettingsAction.RevertDelay(it)); editing = false
    }
    if (details) AppDialog(title = stringResource(R.string.usb_revert_details), onDismissRequest = { details = false },
        confirmLabel = stringResource(R.string.dialog_close), onConfirm = { details = false }, icon = Icons.Outlined.Info) {
        Text(stringResource(R.string.dns_revert_explanation))
    }
}
