package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsInfoRow
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

private enum class ShortcutHelpStep(val title: Int, val summary: Int, val icon: ImageVector) {
    Open(R.string.shortcut_help_step_open_title, R.string.shortcut_help_step_open_summary,
        Icons.Outlined.Apps),
    Run(R.string.shortcut_help_step_run_title, R.string.shortcut_help_step_run_summary,
        Icons.Outlined.TouchApp),
    Pin(R.string.shortcut_help_step_pin_title, R.string.shortcut_help_step_pin_summary,
        Icons.Outlined.PushPin),
    Automate(R.string.shortcut_help_step_automate_title, R.string.shortcut_help_step_automate_summary,
        Icons.Outlined.Autorenew)
}

/**
 * What a shortcut action is and how to run, pin or automate one. The settings list keeps
 * its switches and counts; the device limit named here comes from the same configuration.
 */
@Composable
internal fun ShortcutHelpScreen(
    maxShortcutCount: Int,
    onPermissionHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(stringResource(R.string.shortcut_help_intro), style = MaterialTheme.typography.bodyLarge)
        SettingsGroup(ShortcutHelpStep.entries, itemKey = { it.name },
            title = stringResource(R.string.shortcut_help_steps_title)) { step ->
            SettingsRowContent(stringResource(step.title), stringResource(step.summary), step.icon,
                enabled = true, modifier = Modifier) {}
        }
        SettingsSection(title = stringResource(R.string.shortcut_help_rules_title)) {
            Text(stringResource(R.string.shortcut_settings_description, maxShortcutCount,
                ShortcutCatalog.MAX_FAVORITE_SHORTCUTS), style = MaterialTheme.typography.bodyMedium)
        }
        SettingsGroup(listOf(Unit), itemKey = { "permission" },
            title = stringResource(R.string.shortcut_help_permission_title)) {
            SettingsNavigationRow(stringResource(R.string.menu_permission_help),
                Icons.Outlined.AdminPanelSettings, onPermissionHelp,
                summary = stringResource(R.string.shortcut_help_permission_summary))
        }
        SettingsInfoRow(stringResource(R.string.shortcut_help_launcher_note))
    }
}
