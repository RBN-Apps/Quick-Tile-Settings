package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsListSortMode

@Composable
internal fun DnsListHeader(
    sortMode: DnsListSortMode,
    onSortModeSelected: (DnsListSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.dns_select_hostnames_for_cycle),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag("dns_sort_menu_button")
            ) {
                Icon(
                    imageVector = if (sortMode == DnsListSortMode.MANUAL) {
                        Icons.Filled.DragHandle
                    } else {
                        Icons.Filled.SortByAlpha
                    },
                    contentDescription = stringResource(R.string.dns_sort_content_description)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dns_sort_manual)) },
                    leadingIcon = {
                        Icon(Icons.Filled.DragHandle, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortMode == DnsListSortMode.MANUAL) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortModeSelected(DnsListSortMode.MANUAL)
                    },
                    modifier = Modifier.testTag("dns_sort_manual")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dns_sort_alphabetical)) },
                    leadingIcon = {
                        Icon(Icons.Filled.SortByAlpha, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortMode == DnsListSortMode.ALPHABETICAL) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortModeSelected(DnsListSortMode.ALPHABETICAL)
                    },
                    modifier = Modifier.testTag("dns_sort_alphabetical")
                )
            }
        }
    }
}
