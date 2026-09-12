package com.rbn.qtsettings.feature.shortcuts.presentation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R

@Composable
internal fun QuickActionsDialog(
    tileType: TileType,
    actions: List<QuickAction>,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onActionClick: (QuickAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = when (tileType) {
                    TileType.DNS -> stringResource(R.string.quick_action_dns_title)
                    TileType.USB -> stringResource(R.string.quick_action_usb_title)
                    TileType.ALL -> stringResource(R.string.quick_action_title)
                }
            )
        },
        text = {
            if (actions.isEmpty()) {
                Text(stringResource(R.string.quick_action_no_actions))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(actions) { action ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onActionClick(action) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(action.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenApp) {
                Text(stringResource(R.string.quick_action_open_app))
            }
        }
    )
}

