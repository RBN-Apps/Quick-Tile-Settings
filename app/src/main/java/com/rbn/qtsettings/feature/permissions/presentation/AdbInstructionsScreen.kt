package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppLinkButton
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * The ADB route end to end: five numbered steps that each carry their own action, and below them
 * the install command as a clearly separate alternative rather than a sixth step.
 */
@Composable
fun AdbInstructionsScreen(command: String, onCopy: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(stringResource(R.string.permission_adb_intro), style = MaterialTheme.typography.bodyLarge)
        AdbStepGroup(
            steps = listOf(
                AdbStep(
                    stringResource(R.string.permission_adb_step_computer),
                    stringResource(R.string.permission_adb_computer_summary)
                ) {
                    AppLinkButton(stringResource(R.string.permission_adb_platform_tools),
                        stringResource(R.string.platform_tools_url), Modifier.fillMaxWidth())
                },
                AdbStep(
                    stringResource(R.string.permission_adb_step_device),
                    stringResource(R.string.permission_adb_device_summary)
                ),
                AdbStep(
                    stringResource(R.string.permission_adb_step_connect),
                    stringResource(R.string.permission_adb_connect_summary)
                ),
                AdbStep(
                    stringResource(R.string.permission_adb_step_grant),
                    stringResource(R.string.help_dialog_how_to_grant_via_adb)
                ) {
                    AdbCommandBlock(command, stringResource(R.string.button_copy_grant_command), onCopy)
                },
                AdbStep(
                    stringResource(R.string.permission_adb_step_verify),
                    stringResource(R.string.permission_adb_verify)
                )
            ),
            title = stringResource(R.string.permission_adb_steps_title)
        )
        AdbInstallAlternative(onCopy)
    }
}

/**
 * Reinstalling with `-g` is a different trade-off, not a later step, so it keeps its own group:
 * one card for the command and its cost, one for where the APK comes from.
 */
@Composable
private fun AdbInstallAlternative(onCopy: (String) -> Unit) {
    SettingsGroup(listOf("install", "apk"), itemKey = { it },
        title = stringResource(R.string.permission_adb_alternative_title)) { item ->
        Column {
            when (item) {
                "install" -> {
                    SettingsRowContent(stringResource(R.string.permission_adb_install_title),
                        stringResource(R.string.permission_adb_install_summary),
                        Icons.Outlined.Terminal, true, Modifier) {}
                    AdbCardContent {
                        AdbInstallWarning()
                        AdbCommandBlock(stringResource(R.string.adb_command_install_g),
                            stringResource(R.string.button_copy_install_command), onCopy)
                    }
                }
                else -> {
                    SettingsRowContent(stringResource(R.string.permission_adb_apk_title),
                        stringResource(R.string.help_dialog_apk_path_note),
                        Icons.Outlined.Download, true, Modifier) {}
                    AdbCardContent {
                        AppLinkButton(stringResource(R.string.github_releases_link_text),
                            stringResource(R.string.github_releases_url), Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Everything a card adds below its title row lines up with the row's text, not with its icon. */
@Composable
private fun AdbCardContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(horizontal = AppSpacing.rowHorizontal).padding(bottom = AppSpacing.rowVertical),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap),
        content = content
    )
}

/** `-g` grants every runtime permission the manifest declares, not only the one this page is for. */
@Composable
private fun AdbInstallWarning() {
    Surface(color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(AppSpacing.rowHorizontal),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null,
                modifier = Modifier.size(AppSpacing.iconSize))
            Text(stringResource(R.string.permission_adb_install_warning),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
