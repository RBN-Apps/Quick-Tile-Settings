package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry

@Composable
internal fun DnsHostnameActionsMenu(
    entry: DnsHostnameEntry,
    isActive: Boolean,
    onSetActiveClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    canActivate: Boolean = true
) {
    var actionsExpanded by remember(entry.id) { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { actionsExpanded = true }
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(
                    R.string.dns_entry_actions,
                    entry.name,
                    entry.hostname
                )
            )
        }
        DropdownMenu(
            expanded = actionsExpanded,
            onDismissRequest = { actionsExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isActive) {
                            stringResource(R.string.dns_active)
                        } else {
                            stringResource(R.string.dns_set_active)
                        }
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                },
                enabled = canActivate && !isActive && entry.hostname.isNotBlank(),
                onClick = {
                    actionsExpanded = false
                    onSetActiveClicked()
                }
            )

            if (entry.isPredefined && entry.descriptionResId != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.button_info_dns, entry.name)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                    },
                    onClick = {
                        actionsExpanded = false
                        onInfoClicked()
                    }
                )
            } else if (!entry.isPredefined) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.button_edit)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    },
                    onClick = {
                        actionsExpanded = false
                        onEditClicked()
                    },
                    modifier = Modifier.testTag(
                        "dns_edit_button_${entry.hostname}_${entry.name}"
                    )
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.button_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        actionsExpanded = false
                        onDeleteClicked()
                    },
                    modifier = Modifier.testTag(
                        "dns_delete_button_${entry.hostname}_${entry.name}"
                    )
                )
            }
        }
    }
}
