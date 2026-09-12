package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

@Composable
internal fun ShortcutCountInfo(state: ShortcutSettingsState, onAction: (ShortcutSettingsAction) -> Unit) {
    SettingsGroup(listOf(Unit), itemKey = { "count" }) {
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button) {
            onAction(ShortcutSettingsAction.ToggleInfo)
        }) {
            SettingsRowContent(
                title = stringResource(R.string.shortcut_settings_limit, state.configuration.enabledIds.size, state.configuration.maxCount),
                summary = null, icon = Icons.Outlined.Info, enabled = true,
                modifier = Modifier
            ) {
                Icon(if (state.infoExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            AnimatedVisibility(state.infoExpanded) {
                Text(stringResource(R.string.shortcut_settings_description, state.configuration.maxCount,
                    ShortcutCatalog.MAX_FAVORITE_SHORTCUTS), Modifier.padding(AppSpacing.rowHorizontal))
            }
        }
    }
}
