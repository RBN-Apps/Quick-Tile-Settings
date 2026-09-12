package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.AppLinkButton

@Composable
fun AdbInstructionDialog(onDismissRequest: () -> Unit, onCopyToClipboard: (String) -> Unit) {
    val command = "adb shell pm grant ${LocalContext.current.packageName} android.permission.WRITE_SECURE_SETTINGS"
    AppDialog(stringResource(R.string.adb_instructions_title), onDismissRequest,
        stringResource(R.string.dialog_close), onDismissRequest) {
        Text(stringResource(R.string.adb_instructions_intro))
        AdbCommandSection(stringResource(R.string.help_dialog_method_1_title),
            stringResource(R.string.help_dialog_how_to_grant_via_adb), command,
            stringResource(R.string.button_copy_grant_command), onCopyToClipboard)
        AdbCommandSection(stringResource(R.string.help_dialog_method_2_title),
            stringResource(R.string.help_dialog_how_to_install_via_adb), stringResource(R.string.adb_command_install_g),
            stringResource(R.string.button_copy_install_command), onCopyToClipboard)
        Text(stringResource(R.string.help_dialog_apk_path_note))
        AppLinkButton(stringResource(R.string.github_releases_link_text), stringResource(R.string.github_releases_url),
            Modifier.fillMaxWidth())
        Text(stringResource(R.string.help_dialog_after_grant))
    }
}
