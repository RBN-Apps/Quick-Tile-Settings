package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.SettingsActionRow
import com.rbn.qtsettings.core.designsystem.SettingsGroup

@Composable
internal fun QuickActionsDialog(
    tileType: TileType, actions: List<QuickAction>, onDismiss: () -> Unit,
    onOpenApp: () -> Unit, onActionClick: (QuickAction) -> Unit
) {
    AppDialog(
        title = stringResource(when (tileType) {
            TileType.DNS -> R.string.quick_action_dns_title
            TileType.USB -> R.string.quick_action_usb_title
            TileType.ALL -> R.string.quick_action_title
        }),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.quick_action_open_app), onConfirm = onOpenApp,
        dismissLabel = stringResource(R.string.dialog_cancel)
    ) {
        if (actions.isEmpty()) Text(stringResource(R.string.quick_action_no_actions))
        else SettingsGroup(actions, itemKey = { it.kind }) { action ->
            SettingsActionRow(action.label, action.description, painterResource(action.iconRes),
                onClick = { onActionClick(action) })
        }
    }
}
