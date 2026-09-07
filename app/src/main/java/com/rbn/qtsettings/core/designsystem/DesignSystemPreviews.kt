package com.rbn.qtsettings.core.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme

// Preview fixtures also rendered by the instrumentation gallery. No production destination.
@Composable
internal fun DesignSystemRowsGallery() {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)
    ) {
        SettingsGroup(items = listOf(0, 1, 2, 3), itemKey = { it }) { row ->
            when (row) {
                0 -> SettingsNavigationRow(
                    title = stringResource(R.string.setting_title_private_dns),
                    summary = stringResource(R.string.setting_desc_tile_cycles),
                    status = stringResource(R.string.dns_state_auto),
                    icon = Icons.Outlined.Dns, onClick = {}
                )
                1 -> SettingsNavigationRow(
                    title = stringResource(R.string.setting_title_usb_debugging),
                    summary = stringResource(R.string.setting_also_hide_dev_options_desc),
                    icon = Icons.Outlined.Usb, onClick = {}, enabled = false
                )
                2 -> SettingsSwitchRow(
                    title = stringResource(R.string.setting_require_unlock),
                    summary = stringResource(R.string.setting_require_unlock_desc),
                    icon = Icons.Outlined.Lock, checked = true, onCheckedChange = {}
                )
                3 -> SettingsCheckboxRow(
                    title = stringResource(R.string.setting_also_disable_wireless_debugging),
                    summary = stringResource(R.string.setting_also_disable_wireless_debugging_desc),
                    icon = Icons.Outlined.Wifi, checked = false, onCheckedChange = {}
                )
            }
        }
        SettingsGroup(items = listOf(Unit), itemKey = { "backup" },
            title = stringResource(R.string.app_settings_title)) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_backup_title),
                summary = stringResource(R.string.settings_backup_description),
                icon = Icons.Outlined.Restore, onClick = {}
            )
        }
    }
}

@Composable
internal fun DesignSystemInputsGallery() {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)
    ) {
        AppTextField(
            value = "dns.example.com", onValueChange = {},
            label = stringResource(R.string.dns_hostname_value_label),
            supportingText = stringResource(R.string.setting_desc_tile_cycles),
            modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            value = "https://", onValueChange = {},
            label = stringResource(R.string.dns_hostname_value_label),
            errorText = stringResource(R.string.error_hostname_value_invalid),
            modifier = Modifier.fillMaxWidth()
        )
        AppTextField(value = "", onValueChange = {}, enabled = false,
            label = stringResource(R.string.dns_hostname_name_label), modifier = Modifier.fillMaxWidth())
        AppButton(text = stringResource(R.string.dialog_save), onClick = {}, modifier = Modifier.fillMaxWidth())
        AppButton(text = stringResource(R.string.backup_export_button), onClick = {},
            style = AppButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        AppButton(text = stringResource(R.string.dialog_cancel), onClick = {},
            style = AppButtonStyle.Text, modifier = Modifier.fillMaxWidth())
        AppButton(text = stringResource(R.string.button_delete), onClick = {},
            style = AppButtonStyle.Destructive, modifier = Modifier.fillMaxWidth())
        AppButton(text = stringResource(R.string.dialog_save), onClick = {},
            enabled = false, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Rows · light EN", widthDp = 360, heightDp = 1000, locale = "en")
@Preview(name = "Rows · dark DE", widthDp = 360, heightDp = 1000, locale = "de",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Rows · large text DE", widthDp = 320, heightDp = 1600, locale = "de", fontScale = 2f)
@Preview(name = "Rows · landscape EN", widthDp = 640, heightDp = 360, locale = "en")
@Composable
private fun SettingsRowsPreview() {
    QuickTileSettingsTheme { Surface { DesignSystemRowsGallery() } }
}

@Preview(name = "Inputs · light EN", widthDp = 360, heightDp = 850, locale = "en")
@Preview(name = "Inputs · dark DE", widthDp = 360, heightDp = 850, locale = "de",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Inputs · large text DE", widthDp = 320, heightDp = 1500, locale = "de", fontScale = 2f)
@Composable
private fun SettingsInputsPreview() {
    QuickTileSettingsTheme { Surface { DesignSystemInputsGallery() } }
}
