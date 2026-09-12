package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.feature.dns.presentation.*
import com.rbn.qtsettings.ui.composables.shared.CheckboxItem

@Composable
fun DnsSettingsCard(
    state: DnsSettingsState,
    onAction: (DnsSettingsAction) -> Unit,
    onReorder: (List<String>) -> Boolean,
    automationContent: @Composable () -> Unit
) {
    val dnsToggleOff = state.settings.toggleOff
    val dnsToggleAuto = state.settings.toggleAuto
    val dnsHostnames = state.settings.providers.map { it.toRowEntry() }
    val dnsListSortMode = if (state.settings.manualOrder) DnsListSortMode.MANUAL else DnsListSortMode.ALPHABETICAL
    val activeDnsHostname = (state.active as? DnsTarget.Hostname)?.value
    val enableAutoRevert = state.settings.enableAutoRevert
    val autoRevertDelay = state.settings.autoRevertDelaySeconds
    val dnsRequireUnlock = state.settings.requireUnlock
    val showDnsInfoDialogFor = remember { mutableStateOf<DnsHostnameEntry?>(null) }
    val dnsScrollState = rememberScrollState()
    var dnsScrollViewport by remember { mutableStateOf<Rect?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        val contentColor = LocalContentColor.current
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setting_title_private_dns),
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
                    .onGloballyPositioned { coordinates ->
                        dnsScrollViewport = coordinates.boundsInWindow()
                    }
                    .verticalScroll(dnsScrollState)
            ) {
                DnsModeRow(
                    checked = dnsToggleOff,
                    onCheckedChange = { onAction(DnsSettingsAction.ToggleOff(it)) },
                    label = stringResource(R.string.dns_state_off),
                    isActive = state.active == DnsTarget.Off,
                    onSetActiveClicked = { onAction(DnsSettingsAction.Activate(DnsTarget.Off)) }
                )
                DnsModeRow(
                    checked = dnsToggleAuto,
                    onCheckedChange = { onAction(DnsSettingsAction.ToggleAuto(it)) },
                    label = stringResource(R.string.dns_state_auto),
                    isActive = state.active == DnsTarget.Automatic,
                    onSetActiveClicked = { onAction(DnsSettingsAction.Activate(DnsTarget.Automatic)) }
                )

                DnsListHeader(
                    sortMode = dnsListSortMode,
                    onSortModeSelected = { onAction(DnsSettingsAction.ManualOrder(it == DnsListSortMode.MANUAL)) },
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                if (dnsListSortMode == DnsListSortMode.MANUAL) {
                    Text(
                        text = stringResource(R.string.dns_manual_reorder_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                DnsHostnameList(
                    entries = dnsHostnames,
                    sortMode = dnsListSortMode,
                    activeDnsHostname = activeDnsHostname,
                    scrollState = dnsScrollState,
                    scrollViewport = dnsScrollViewport,
                    onSelectionChanged = { id, selected -> onAction(DnsSettingsAction.Selected(id, selected)) },
                    onOrderCommitted = onReorder,
                    onSetActive = { entry -> onAction(DnsSettingsAction.Activate(DnsTarget.Hostname(entry.hostname), entry.name)) },
                    onEdit = { onAction(DnsSettingsAction.Edit(it.id)) },
                    onDelete = { onAction(DnsSettingsAction.Delete(it?.id)) },
                    onInfo = { entry -> showDnsInfoDialogFor.value = entry }
                )

                OutlinedButton(
                    onClick = { onAction(DnsSettingsAction.Edit(null)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.dns_add_custom_hostname_button)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.dns_add_custom_hostname_button))
                }

                if (dnsHostnames.any { it.isSelectedForCycle && it.hostname.isBlank() }) {
                    Text(
                        text = stringResource(R.string.warning_hostname_blank),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                automationContent()

                // Auto-Revert Section
                val interactionSourceAutoRevert = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSourceAutoRevert,
                            indication = null,
                            onClick = { onAction(DnsSettingsAction.EnableAutoRevert(!enableAutoRevert)) }
                        )
                ) {
                    Checkbox(
                        checked = enableAutoRevert,
                        onCheckedChange = { onAction(DnsSettingsAction.EnableAutoRevert(it)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setting_enable_auto_revert),
                        style = MaterialTheme.typography.titleMedium,
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
                        color = contentColor.copy(
                            alpha = if (enableAutoRevert) 1f else 0.38f
                        )
                    )
                    OutlinedTextField(
                        value = autoRevertDelay.toString(),
                        onValueChange = { value ->
                            val newDelay =
                                value.toIntOrNull() ?: autoRevertDelay
                            onAction(DnsSettingsAction.RevertDelay(newDelay))
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = enableAutoRevert
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Require Unlock Section
                CheckboxItem(
                    checked = dnsRequireUnlock,
                    onCheckedChange = { onAction(DnsSettingsAction.RequireUnlock(it)) },
                    label = stringResource(R.string.setting_require_unlock),
                    summary = stringResource(R.string.setting_require_unlock_desc)
                )
            }
        }
    }
    showDnsInfoDialogFor.value?.let { entry ->
        if (entry.isPredefined && entry.descriptionResId != null) {
            DnsInfoDialog(
                entry = entry,
                onDismissRequest = { showDnsInfoDialogFor.value = null }
            )
        }
    }

}
