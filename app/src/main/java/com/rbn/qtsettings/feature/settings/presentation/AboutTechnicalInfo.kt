package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.platform.app.AppInfo

@Composable
internal fun AboutTechnicalInfo(info: AppInfo?, onCopy: (String) -> Unit) {
    val unknown = stringResource(if (info == null) R.string.about_loading else R.string.about_unknown)
    val rows = listOf(
        stringResource(R.string.about_version_label) to (info?.let {
            "${it.versionName ?: unknown} (${it.versionCode ?: unknown})" } ?: unknown),
        stringResource(R.string.about_package_label) to (info?.packageName ?: unknown),
        stringResource(R.string.about_android_label) to (info?.let { "${it.androidVersion} / API ${it.apiLevel}" } ?: unknown),
        stringResource(R.string.about_device_label) to (info?.let { "${it.manufacturer} ${it.model}" } ?: unknown),
        stringResource(R.string.about_abi_label) to (info?.abis?.takeIf { it.isNotBlank() } ?: unknown)
    )
    SettingsSection(title = stringResource(R.string.about_technical_title)) {
        SettingsGroup(items = rows, itemKey = { it.first }) { (title, value) ->
            SettingsRowContent(title, value, null, true, Modifier) {}
        }
        if (info != null) AppButton(stringResource(R.string.about_copy_technical),
            { onCopy(rows.joinToString("\n") { "${it.first}: ${it.second}" }) },
            style = AppButtonStyle.Secondary, icon = Icons.Outlined.ContentCopy)
    }
}
