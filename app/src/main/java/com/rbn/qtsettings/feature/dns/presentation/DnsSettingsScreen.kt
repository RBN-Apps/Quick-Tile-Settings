package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsStatusRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.dns.domain.DnsTarget

@Composable
fun DnsSettingsScreen(state: DnsSettingsState, onAction: (DnsSettingsAction) -> Unit, onOpen: (AppDestination) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        SettingsGroup(items = listOf(0), itemKey = { it }) {
            val status = dnsStatus(state)
            SettingsStatusRow(
                status.label, status.icon, status = status.hostname,
                stateLabel = stringResource(R.string.status_current_state),
                actionLabel = stringResource(R.string.status_cycle_action),
                reserveStatusLine = true,
                enabled = state.hasPermission,
                onClick = { onAction(DnsSettingsAction.Cycle) }
            )
        }
        SettingsGroup(items = listOf(1, 2, 3, 4, 5), itemKey = { it }) { item ->
            when (item) {
                1 -> SettingsNavigationRow(stringResource(R.string.dns_menu_providers), Icons.Outlined.Dns,
                    { onOpen(AppDestination.DnsProviders) }, summary = stringResource(R.string.dns_menu_providers_summary))
                2 -> SettingsNavigationRow(stringResource(R.string.dns_menu_automation), Icons.Outlined.SettingsEthernet,
                    { onOpen(AppDestination.DnsAutomation) }, summary = stringResource(R.string.dns_menu_automation_summary))
                3 -> SettingsNavigationRow(stringResource(R.string.usb_menu_revert), Icons.Outlined.Timer,
                    { onOpen(AppDestination.DnsRevert) }, summary = stringResource(R.string.dns_menu_revert_summary),
                    status = if (state.settings.enableAutoRevert && state.settings.autoRevertDelaySeconds > 0)
                        pluralStringResource(R.plurals.usb_delay_seconds, state.settings.autoRevertDelaySeconds, state.settings.autoRevertDelaySeconds)
                    else stringResource(R.string.off_state))
                4 -> SettingsNavigationRow(
                    stringResource(R.string.shortcut_settings_title), Icons.AutoMirrored.Outlined.Shortcut,
                    { onOpen(AppDestination.ShortcutSettings) },
                    summary = stringResource(R.string.shortcut_menu_summary)
                )
                5 -> SettingsSwitchRow(stringResource(R.string.setting_require_unlock), state.settings.requireUnlock,
                    { onAction(DnsSettingsAction.RequireUnlock(it)) }, summary = stringResource(R.string.setting_require_unlock_desc), icon = Icons.Outlined.Lock)
            }
        }
    }
}

private data class DnsStatus(val label: String, val hostname: String?, val icon: Painter)

/** Shows the same state glyph as the tile, so the row never repeats the category icon below it. */
@Composable
private fun dnsStatus(state: DnsSettingsState): DnsStatus {
    val active = state.active
    val icon = dnsIconForTarget(active)
        ?.let { painterResource(it) } ?: rememberVectorPainter(Icons.AutoMirrored.Outlined.HelpOutline)
    val provider = (active as? DnsTarget.Hostname)?.let { target ->
        state.settings.providers.find { it.target.value.equals(target.value, ignoreCase = true) }
    }
    return when (active) {
        DnsTarget.Off -> DnsStatus(stringResource(R.string.dns_state_off), null, icon)
        DnsTarget.Automatic -> DnsStatus(stringResource(R.string.dns_state_auto), null, icon)
        is DnsTarget.Hostname -> DnsStatus(provider?.name ?: active.value, provider?.let { active.value }, icon)
        null -> DnsStatus(stringResource(R.string.dns_status_unavailable), null, icon)
    }
}
