package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.SettingsCycleEntryRow
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.feature.dns.presentation.DnsHostnameList
import com.rbn.qtsettings.feature.dns.presentation.DnsInfoDialog
import com.rbn.qtsettings.feature.dns.presentation.DnsListHeader

@Composable
fun DnsProvidersScreen(
    state: DnsSettingsState,
    onAction: (DnsSettingsAction) -> Unit,
    onReorder: (List<String>) -> Boolean
) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }
    var infoId by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = state.settings.providers.map { it.toRowEntry() }
    val sortMode = if (state.settings.manualOrder) DnsListSortMode.MANUAL else DnsListSortMode.ALPHABETICAL
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
        Column(
            Modifier.weight(1f)
                .onGloballyPositioned { viewport = it.boundsInWindow() }
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)
        ) {
            SettingsSection(title = stringResource(R.string.dns_cycle_title)) {
                Text(stringResource(R.string.dns_cycle_explanation), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingsGroup(items = listOf(DnsTarget.Off, DnsTarget.Automatic), itemKey = { it.toString() }) { target ->
                    val off = target == DnsTarget.Off
                    val label = stringResource(if (off) R.string.dns_state_off else R.string.dns_state_auto)
                    SettingsCycleEntryRow(
                        title = label,
                        checked = if (off) state.settings.toggleOff else state.settings.toggleAuto,
                        onCheckedChange = {
                            onAction(if (off) DnsSettingsAction.ToggleOff(it) else DnsSettingsAction.ToggleAuto(it))
                        },
                        isActive = state.active == target,
                        activeLabel = stringResource(R.string.dns_active),
                        activateLabel = stringResource(R.string.dns_set_active_mode, label),
                        onActivate = { onAction(DnsSettingsAction.Activate(target)) },
                        icon = ImageVector.vectorResource(
                            if (off) R.drawable.ic_dns_off else R.drawable.ic_dns_auto
                        ),
                        activateEnabled = state.hasPermission
                    )
                }
            }
            SettingsSection {
                DnsListHeader(sortMode, { onAction(DnsSettingsAction.ManualOrder(it == DnsListSortMode.MANUAL)) })
                if (state.settings.manualOrder) Text(stringResource(R.string.dns_manual_reorder_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entries.isEmpty()) Text(stringResource(R.string.dns_providers_empty), style = MaterialTheme.typography.bodyMedium)
                // Keep the existing measured drag/reorder algorithm and its accessibility actions.
                Column {
                    DnsHostnameList(
                        entries = entries,
                        sortMode = sortMode,
                        activeDnsHostname = (state.active as? DnsTarget.Hostname)?.value,
                        scrollState = scroll,
                        scrollViewport = viewport,
                        onSelectionChanged = { id, selected -> onAction(DnsSettingsAction.Selected(id, selected)) },
                        onOrderCommitted = onReorder,
                        onSetActive = { onAction(DnsSettingsAction.Activate(DnsTarget.Hostname(it.hostname), it.name)) },
                        onEdit = { onAction(DnsSettingsAction.Edit(it.id)) },
                        onDelete = { onAction(DnsSettingsAction.Delete(it?.id)) },
                        onInfo = { infoId = it.id },
                        canActivate = state.hasPermission
                    )
                }
                if (!state.settings.toggleOff && !state.settings.toggleAuto && entries.none { it.isSelectedForCycle })
                    Text(stringResource(R.string.toast_no_states_enabled_dns), color = MaterialTheme.colorScheme.error)
                if (entries.any { it.isSelectedForCycle && it.hostname.isBlank() })
                    Text(stringResource(R.string.warning_hostname_blank), color = MaterialTheme.colorScheme.error)
            }
        }
        AppButton(stringResource(R.string.dns_add_custom_hostname_button), { onAction(DnsSettingsAction.Edit(null)) },
            Modifier.fillMaxWidth(), style = AppButtonStyle.Secondary, icon = Icons.Filled.Add)
    }
    entries.find { it.id == infoId }?.let { DnsInfoDialog(it) { infoId = null } }
}
