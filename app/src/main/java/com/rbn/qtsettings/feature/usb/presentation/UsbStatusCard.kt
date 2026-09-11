package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R


@Composable
internal fun UsbStatusCard(state: UsbSettingsState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.setting_title_usb_debugging), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (state.system.active) R.string.usb_status_on else R.string.usb_status_off),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(stringResource(R.string.usb_status_summary), style = MaterialTheme.typography.bodyMedium)
            if (!state.system.developerOptions) Text(
                stringResource(R.string.usb_dev_options_off),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
