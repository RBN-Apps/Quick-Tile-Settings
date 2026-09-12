package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION

@Composable
internal fun VpnAutomationSection(state: AutomationUiState, viewModel: AutomationUiActions) {
    val vpnDetectionEnabled = state.vpnDetectionEnabled
    val vpnDetectionMode = state.vpnDetectionMode
    // VPN Detection Section
    val interactionSourceVpnDetection = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSourceVpnDetection,
                indication = null,
                onClick = { viewModel.setVpnDetectionEnabled(!vpnDetectionEnabled) }
            )
    ) {
        Checkbox(
            checked = vpnDetectionEnabled,
            onCheckedChange = { viewModel.setVpnDetectionEnabled(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.setting_vpn_detection_enabled),
            style = MaterialTheme.typography.titleMedium,
        )
    }

    if (vpnDetectionEnabled) {
        Text(
            text = stringResource(R.string.setting_vpn_detection_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = vpnDetectionMode == TILE_ONLY_DETECTION,
                    onClick = { viewModel.setVpnDetectionMode(TILE_ONLY_DETECTION) }
                )
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = vpnDetectionMode == TILE_ONLY_DETECTION,
                onClick = { viewModel.setVpnDetectionMode(TILE_ONLY_DETECTION) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.vpn_detection_tile_only_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.vpn_detection_tile_only_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = vpnDetectionMode == BACKGROUND_DETECTION,
                    onClick = { viewModel.setVpnDetectionMode(BACKGROUND_DETECTION) }
                )
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = vpnDetectionMode == BACKGROUND_DETECTION,
                onClick = { viewModel.setVpnDetectionMode(BACKGROUND_DETECTION) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.vpn_detection_background_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.vpn_detection_background_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

}
