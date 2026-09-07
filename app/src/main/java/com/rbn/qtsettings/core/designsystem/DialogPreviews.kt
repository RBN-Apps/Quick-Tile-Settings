package com.rbn.qtsettings.core.designsystem

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.ui.composables.dns.ConfirmDeleteDialog
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutPinnedInfoDialog

@Preview(name = "Info · EN light", locale = "en", widthDp = 360)
@Preview(name = "Info · DE dark", locale = "de", widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun InfoDialogPreview() {
    QuickTileSettingsTheme { ShortcutPinnedInfoDialog(onDismissRequest = {}) }
}

@Preview(name = "Confirmation · EN", locale = "en", widthDp = 360)
@Composable
internal fun ConfirmationDialogPreview() {
    QuickTileSettingsTheme {
        AppConfirmationDialog(
            title = stringResource(R.string.shortcut_settings_allow_pinned_warning_title),
            message = stringResource(R.string.shortcut_settings_allow_pinned_warning_message),
            confirmLabel = stringResource(R.string.shortcut_settings_allow_pinned_warning_confirm),
            onConfirm = {},
            onDismissRequest = {}
        )
    }
}

@Preview(name = "Destructive · DE", locale = "de", widthDp = 360)
@Preview(name = "Destructive · EN dark", locale = "en", widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DeleteDialogPreview() {
    QuickTileSettingsTheme {
        ConfirmDeleteDialog(
            hostnameEntry = DnsHostnameEntry(
                id = "preview", name = "Home DNS", hostname = "dns.example.com", isPredefined = false
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "Long content · DE 200%", locale = "de", widthDp = 320,
    heightDp = 640, fontScale = 2f)
@Preview(name = "Long content · EN landscape", locale = "en", widthDp = 640,
    heightDp = 360)
@Composable
internal fun LongDialogPreview() {
    QuickTileSettingsTheme {
        AppInfoDialog(
            title = stringResource(R.string.network_type_info_title),
            message = stringResource(R.string.network_type_info_message),
            icon = Icons.Outlined.Info,
            onDismissRequest = {}
        )
    }
}

@Preview(name = "Form error / disabled · DE", locale = "de", widthDp = 320)
@Preview(name = "Form error / disabled · EN dark", locale = "en", widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun FormDialogPreview() {
    val hostnameLabel = stringResource(R.string.dns_hostname_value_label)
    val hostnameError = stringResource(R.string.error_hostname_value_invalid)
    val explanation = stringResource(R.string.setting_network_type_detection_description)
    QuickTileSettingsTheme {
        AppDialog(
            title = stringResource(R.string.dns_add_hostname_title),
            onDismissRequest = {},
            confirmLabel = stringResource(R.string.dialog_save),
            onConfirm = {},
            dismissLabel = stringResource(R.string.dialog_cancel),
            confirmEnabled = false
        ) {
            AppTextField(
                value = "https://dns.example.com",
                onValueChange = {},
                label = hostnameLabel,
                errorText = hostnameError
            )
            Text(explanation)
        }
    }
}
