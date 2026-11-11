package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.SettingsChoiceRow
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION

@Composable
fun AutomationModeDialog(mode: String, vpn: Boolean, requiresBackground: Boolean,
                         onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AppDialog(stringResource(R.string.automation_detection_mode), onDismiss,
        stringResource(R.string.dialog_cancel), onDismiss, icon = Icons.Outlined.Radar) {
        Column(Modifier.selectableGroup()) {
            SettingsChoiceRow(stringResource(R.string.automation_mode_tile), mode == TILE_ONLY_DETECTION,
                { onSelect(TILE_ONLY_DETECTION) },
                modifier = Modifier.testTag(if (vpn) "vpn_detection_tile_only_option" else "network_type_detection_tile_only_option"),
                summary = stringResource(if (requiresBackground) R.string.wifi_rules_require_background_detection
                    else if (vpn) R.string.automation_vpn_tile_description else R.string.automation_network_tile_description),
                enabled = !requiresBackground)
            SettingsChoiceRow(stringResource(R.string.automation_mode_background), mode == BACKGROUND_DETECTION,
                { onSelect(BACKGROUND_DETECTION) },
                modifier = Modifier.testTag(if (vpn) "vpn_detection_background_option" else "network_type_detection_background_option"),
                summary = stringResource(R.string.automation_background_description))
        }
    }
}
