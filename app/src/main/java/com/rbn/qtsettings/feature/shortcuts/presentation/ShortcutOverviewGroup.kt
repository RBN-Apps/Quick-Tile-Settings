package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

/** The counts stay a status line; the explanation is too long to unfold over the list below it. */
@Composable
internal fun ShortcutOverviewGroup(state: ShortcutSettingsState, onOpenHelp: () -> Unit) {
    SettingsGroup(listOf("count", "help"), itemKey = { it }) { item ->
        if (item == "count") {
            SettingsRowContent(
                title = stringResource(R.string.shortcut_settings_limit,
                    state.configuration.enabledIds.size, state.configuration.maxCount),
                summary = stringResource(R.string.shortcut_favorites_count,
                    state.configuration.favoriteIds.size, ShortcutCatalog.MAX_FAVORITE_SHORTCUTS),
                icon = Icons.Outlined.Info, enabled = true, modifier = Modifier
            ) {}
        } else {
            SettingsNavigationRow(stringResource(R.string.shortcut_help_title),
                Icons.AutoMirrored.Outlined.HelpOutline, onOpenHelp,
                summary = stringResource(R.string.shortcut_help_entry_summary))
        }
    }
}
