package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION

@Composable
fun AutomationScreen(state: AutomationUiState, actions: AutomationUiActions,
    providers: List<DnsHostnameEntry>, onOpen: (String) -> Unit, onNotificationAccess: () -> Unit,
    onOpenRules: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        NetworkAutomationSection(state, actions, providers, onOpen, onOpenRules)
        if (state.networkTypeDetectionEnabled && state.networkTypeDetectionMode == BACKGROUND_DETECTION && !state.notificationsAllowed)
            NotificationStatusHint(onNotificationAccess)
        VpnAutomationSection(state, actions, onOpen)
        if (state.vpnDetectionEnabled && state.vpnDetectionMode == BACKGROUND_DETECTION && !state.notificationsAllowed)
            NotificationStatusHint(onNotificationAccess)
    }
}

@Composable
private fun NotificationStatusHint(onClick: () -> Unit) {
    SettingsGroup(listOf(0), { it }) {
        SettingsNavigationRow(stringResource(R.string.automation_notification_title), Icons.Outlined.NotificationsOff,
            onClick, summary = stringResource(if (android.os.Build.VERSION.SDK_INT >= 33)
                R.string.automation_notification_optional_summary else R.string.automation_notification_legacy_summary))
    }
}
