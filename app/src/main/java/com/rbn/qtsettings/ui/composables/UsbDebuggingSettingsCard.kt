package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun UsbDebuggingSettingsCard(viewModel: MainViewModel, isDevOptionsEnabled: Boolean) {
    val usbToggleEnable by viewModel.usbToggleEnable.collectAsState()
    val usbToggleDisable by viewModel.usbToggleDisable.collectAsState()

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
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                label = stringResource(R.string.usb_state_on)
            )
            CheckboxItem(
                checked = usbToggleDisable,
                onCheckedChange = { viewModel.setUsbToggleDisable(it) },
                label = stringResource(R.string.usb_state_off)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!usbToggleDisable && !usbToggleEnable) {
                Text(
                    text = stringResource(R.string.warning_at_least_one_usb_option),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}