package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
fun DnsSettingsScreen(state: DnsSettingsState, onAction: (DnsSettingsAction) -> Unit, onOpen: (AppDestination) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        DnsStatusCard(state)
        SettingsGroup(items = listOf(0, 1, 2, 3), itemKey = { it }) { item ->
            when (item) {
                0 -> SettingsNavigationRow(stringResource(R.string.dns_menu_providers), Icons.Outlined.Dns,
                    { onOpen(AppDestination.DnsProviders) }, summary = stringResource(R.string.dns_menu_providers_summary))
                1 -> SettingsNavigationRow(stringResource(R.string.dns_menu_automation), Icons.Outlined.SettingsEthernet,
                    { onOpen(AppDestination.DnsAutomation) }, summary = stringResource(R.string.dns_menu_automation_summary))
                2 -> SettingsNavigationRow(stringResource(R.string.usb_menu_revert), Icons.Outlined.Timer,
                    { onOpen(AppDestination.DnsRevert) }, summary = stringResource(R.string.dns_menu_revert_summary),
                    status = if (state.settings.enableAutoRevert && state.settings.autoRevertDelaySeconds > 0)
                        pluralStringResource(R.plurals.usb_delay_seconds, state.settings.autoRevertDelaySeconds, state.settings.autoRevertDelaySeconds)
                    else stringResource(R.string.off_state))
                3 -> SettingsSwitchRow(stringResource(R.string.setting_require_unlock), state.settings.requireUnlock,
                    { onAction(DnsSettingsAction.RequireUnlock(it)) }, summary = stringResource(R.string.setting_require_unlock_desc), icon = Icons.Outlined.Lock)
            }
        }
    }
}
