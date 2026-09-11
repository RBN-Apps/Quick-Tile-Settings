package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow

@Composable
fun UsbAdditionalActionsScreen(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        UsbAdditionalActionsContent(state, onAction)
    }
}

@Composable
internal fun UsbAdditionalActionsContent(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    SettingsGroup(items = listOf(0, 1), itemKey = { it }) { item ->
        if (item == 0) SettingsSwitchRow(
            stringResource(R.string.setting_also_hide_dev_options),
            state.settings.alsoHideDevOptions, { onAction(UsbSettingsAction.AlsoHideDevOptions(it)) },
            summary = stringResource(R.string.setting_also_hide_dev_options_desc)
        )
        else SettingsSwitchRow(
            stringResource(R.string.setting_also_disable_wireless_debugging),
            state.settings.alsoDisableWirelessDebugging,
            { onAction(UsbSettingsAction.AlsoDisableWirelessDebugging(it)) },
            summary = stringResource(R.string.setting_also_disable_wireless_debugging_desc)
        )
    }
}
