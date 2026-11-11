package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow

@Composable
internal fun ShortcutPinnedFallbackRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onInfoClick: () -> Unit) {
    val infoDescription = stringResource(R.string.shortcut_settings_allow_pinned_info_desc)
    SettingsGroup(listOf("toggle", "info"), itemKey = { it }) { item ->
        if (item == "toggle") {
            SettingsSwitchRow(stringResource(R.string.shortcut_settings_allow_pinned_when_disabled),
                checked, onCheckedChange, icon = Icons.Outlined.LockOpen)
        } else {
            SettingsNavigationRow(stringResource(R.string.shortcut_settings_pinned_details), Icons.Outlined.Info,
                onInfoClick, Modifier.semantics { contentDescription = infoDescription })
        }
    }
}
