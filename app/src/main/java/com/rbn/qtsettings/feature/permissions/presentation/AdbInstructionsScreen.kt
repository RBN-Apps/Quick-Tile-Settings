package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppLinkButton
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.SettingsSection

@Composable
fun AdbInstructionsScreen(command: String, onCopy: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.permission_adb_intro))
        SettingsGroup(listOf(0, 1, 2), itemKey = { it }) { step ->
            val title = when (step) {
                0 -> R.string.permission_adb_step_computer
                1 -> R.string.permission_adb_step_device
                else -> R.string.permission_adb_step_connect
            }
            val description = when (step) {
                0 -> R.string.permission_adb_computer_summary
                1 -> R.string.permission_adb_device_summary
                else -> R.string.permission_adb_connect_summary
            }
            SettingsRowContent(stringResource(title), stringResource(description), null, true, Modifier) {}
        }
        AppLinkButton(stringResource(R.string.permission_adb_platform_tools),
            stringResource(R.string.platform_tools_url), Modifier.fillMaxWidth())
        AdbCommandSection(stringResource(R.string.permission_adb_step_grant),
            stringResource(R.string.help_dialog_how_to_grant_via_adb), command,
            stringResource(R.string.button_copy_grant_command), onCopy)
        Text(stringResource(R.string.permission_adb_verify))
        SettingsSection {
            AdbCommandSection(stringResource(R.string.permission_adb_install_title),
                stringResource(R.string.permission_adb_install_summary), stringResource(R.string.adb_command_install_g),
                stringResource(R.string.button_copy_install_command), onCopy)
            Text(stringResource(R.string.help_dialog_apk_path_note))
            AppLinkButton(stringResource(R.string.github_releases_link_text), stringResource(R.string.github_releases_url),
                Modifier.fillMaxWidth())
        }
    }
}
