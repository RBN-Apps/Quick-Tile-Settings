package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsRowContent

@Composable
internal fun AboutFeatures(onShortcuts: () -> Unit, onSettings: () -> Unit) {
    SettingsGroup(items = listOf(0, 1, 2, 3, 4, 5), itemKey = { it },
        title = stringResource(R.string.about_features_title)) { item ->
        val (title, summary, icon) = when (item) {
            0 -> Triple(R.string.tab_title_dns, R.string.about_dns_details, Icons.Outlined.Dns)
            1 -> Triple(R.string.tab_title_usb, R.string.about_usb_details, Icons.Outlined.Usb)
            2 -> Triple(R.string.dns_menu_automation, R.string.about_automation_details, Icons.Outlined.Autorenew)
            3 -> Triple(R.string.about_safety_title, R.string.about_safety_details, Icons.Outlined.Timer)
            4 -> Triple(R.string.shortcut_settings_title, R.string.about_shortcuts_details, Icons.AutoMirrored.Outlined.Shortcut)
            else -> Triple(R.string.settings_backup_title, R.string.about_backup_details, Icons.Outlined.Backup)
        }
        if (item >= 4) SettingsNavigationRow(stringResource(title), icon,
            if (item == 4) onShortcuts else onSettings, summary = stringResource(summary))
        else SettingsRowContent(stringResource(title), stringResource(summary), icon, true, Modifier) {}
    }
}
