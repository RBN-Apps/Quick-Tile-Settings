package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState
import com.rbn.qtsettings.ui.composables.shared.CheckboxItem

@Composable
fun UsbSettingsScreen(state: UsbSettingsState, onAction: (UsbSettingsAction) -> Unit) {
    val usbToggleEnable = state.settings.toggleEnable
    val usbToggleDisable = state.settings.toggleDisable
    val alsoHideDevOptions = state.settings.alsoHideDevOptions
    val alsoDisableWirelessDebugging = state.settings.alsoDisableWirelessDebugging
    val enableAutoRevert = state.settings.enableAutoRevert
    val autoRevertDelay = state.settings.autoRevertDelaySeconds
    val usbRequireUnlock = state.settings.requireUnlock
    val usbDebuggingEnabled = state.system.active
    val isDevOptionsEnabled = state.system.developerOptions
    val canSetActiveUsbState = state.hasPermission && (isDevOptionsEnabled || alsoHideDevOptions)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setting_title_usb_debugging),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.setting_desc_tile_cycles),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {

                if (!isDevOptionsEnabled) {
                    Text(
                        text = stringResource(R.string.warning_developer_options_disabled_config),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                UsbModeRow(
                    checked = usbToggleEnable,
                    onCheckedChange = { onAction(UsbSettingsAction.ToggleEnable(it)) },
                    label = stringResource(R.string.usb_state_on),
                    configurationEnabled = isDevOptionsEnabled || alsoHideDevOptions,
                    isActive = usbDebuggingEnabled,
                    setActiveEnabled = canSetActiveUsbState,
                    onSetActiveClicked = {
                        onAction(UsbSettingsAction.SetActive(true))
                    }
                )
                UsbModeRow(
                    checked = usbToggleDisable,
                    onCheckedChange = { onAction(UsbSettingsAction.ToggleDisable(it)) },
                    label = stringResource(R.string.usb_state_off),
                    configurationEnabled = isDevOptionsEnabled || alsoHideDevOptions,
                    isActive = !usbDebuggingEnabled,
                    setActiveEnabled = canSetActiveUsbState,
                    onSetActiveClicked = {
                        onAction(UsbSettingsAction.SetActive(false))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!usbToggleDisable && !usbToggleEnable && (isDevOptionsEnabled || alsoHideDevOptions)) {
                    Text(
                        text = stringResource(R.string.warning_at_least_one_usb_option),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Also Hide Developer Options Section
                CheckboxItem(
                    checked = alsoHideDevOptions,
                    onCheckedChange = { onAction(UsbSettingsAction.AlsoHideDevOptions(it)) },
                    label = stringResource(R.string.setting_also_hide_dev_options),
                    summary = stringResource(R.string.setting_also_hide_dev_options_desc)
                )

                // Also Disable Wireless Debugging Section
                CheckboxItem(
                    checked = alsoDisableWirelessDebugging,
                    onCheckedChange = { onAction(UsbSettingsAction.AlsoDisableWirelessDebugging(it)) },
                    label = stringResource(R.string.setting_also_disable_wireless_debugging),
                    summary = stringResource(R.string.setting_also_disable_wireless_debugging_desc)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                UsbAutoRevertSection(enableAutoRevert, autoRevertDelay,
                    isDevOptionsEnabled || alsoHideDevOptions, onAction)

                // Require Unlock Section
                CheckboxItem(
                    checked = usbRequireUnlock,
                    onCheckedChange = { onAction(UsbSettingsAction.RequireUnlock(it)) },
                    label = stringResource(R.string.setting_require_unlock),
                    summary = stringResource(R.string.setting_require_unlock_desc)
                )
            }
        }
    }
}


@Preview(name = "USB enabled", showBackground = true)
@Preview(name = "USB dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UsbSettingsPreview() {
    QuickTileSettingsTheme {
        UsbSettingsScreen(UsbSettingsState(system = UsbSystemState(true, true, false), hasPermission = true)) {}
    }
}

@Preview(name = "Developer options disabled", showBackground = true)
@Composable
private fun UsbSettingsDisabledPreview() {
    QuickTileSettingsTheme { UsbSettingsScreen(UsbSettingsState()) {} }
}
