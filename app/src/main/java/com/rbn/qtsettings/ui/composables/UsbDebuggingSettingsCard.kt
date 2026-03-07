package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun UsbDebuggingSettingsCard(viewModel: MainViewModel, isDevOptionsEnabled: Boolean) {
    val usbToggleEnable by viewModel.usbToggleEnable.collectAsState()
    val usbToggleDisable by viewModel.usbToggleDisable.collectAsState()
    val alsoHideDevOptions by viewModel.usbAlsoHideDevOptions.collectAsState()
    val alsoDisableWirelessDebugging by viewModel.usbAlsoDisableWirelessDebugging.collectAsState()
    val enableAutoRevert by viewModel.usbEnableAutoRevert.collectAsState()
    val autoRevertDelay by viewModel.usbAutoRevertDelaySeconds.collectAsState()
    val usbRequireUnlock by viewModel.usbRequireUnlock.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                CheckboxItem(
                    checked = usbToggleEnable,
                    onCheckedChange = { viewModel.setUsbToggleEnable(it) },
                    label = stringResource(R.string.usb_state_on),
                    enabled = isDevOptionsEnabled || alsoHideDevOptions
                )
                CheckboxItem(
                    checked = usbToggleDisable,
                    onCheckedChange = { viewModel.setUsbToggleDisable(it) },
                    label = stringResource(R.string.usb_state_off),
                    enabled = isDevOptionsEnabled || alsoHideDevOptions
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
                    onCheckedChange = { viewModel.setUsbAlsoHideDevOptions(it) },
                    label = stringResource(R.string.setting_also_hide_dev_options)
                )
                Text(
                    text = stringResource(R.string.setting_also_hide_dev_options_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                )

                // Also Disable Wireless Debugging Section
                CheckboxItem(
                    checked = alsoDisableWirelessDebugging,
                    onCheckedChange = { viewModel.setUsbAlsoDisableWirelessDebugging(it) },
                    label = stringResource(R.string.setting_also_disable_wireless_debugging)
                )
                Text(
                    text = stringResource(R.string.setting_also_disable_wireless_debugging_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Revert Section
                val interactionSourceAutoRevert = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSourceAutoRevert,
                            indication = null,
                            onClick = {
                                if (isDevOptionsEnabled || alsoHideDevOptions) viewModel.setUsbEnableAutoRevert(
                                    !enableAutoRevert
                                )
                            },
                            enabled = isDevOptionsEnabled || alsoHideDevOptions
                        )
                ) {
                    Checkbox(
                        checked = enableAutoRevert,
                        onCheckedChange = { viewModel.setUsbEnableAutoRevert(it) },
                        enabled = isDevOptionsEnabled || alsoHideDevOptions
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setting_enable_auto_revert),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDevOptionsEnabled || alsoHideDevOptions) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_auto_revert_delay),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (enableAutoRevert && (isDevOptionsEnabled || alsoHideDevOptions)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                    OutlinedTextField(
                        value = autoRevertDelay.toString(),
                        onValueChange = { value ->
                            val newDelay =
                                value.toIntOrNull() ?: viewModel.usbAutoRevertDelaySeconds.value
                            viewModel.setUsbAutoRevertDelaySeconds(newDelay)
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = enableAutoRevert && (isDevOptionsEnabled || alsoHideDevOptions)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Require Unlock Section
                CheckboxItem(
                    checked = usbRequireUnlock,
                    onCheckedChange = { viewModel.setUsbRequireUnlock(it) },
                    label = stringResource(R.string.setting_require_unlock)
                )
                Text(
                    text = stringResource(R.string.setting_require_unlock_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                )
            }
        }
    }
}