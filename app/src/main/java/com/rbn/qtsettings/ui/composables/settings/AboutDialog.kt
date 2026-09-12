package com.rbn.qtsettings.ui.composables.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*

@Composable
fun AboutDialog(onDismissRequest: () -> Unit, onOpenPermissionDialog: () -> Unit) {
    val context = LocalContext.current
    val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown" }
        catch (_: Exception) { "Unknown" }
    AppDialog(stringResource(R.string.app_name), onDismissRequest, stringResource(R.string.dialog_close),
        onDismissRequest, icon = Icons.Outlined.Info) {
        Text(stringResource(R.string.about_version, version))
        Text(stringResource(R.string.about_description))
        SettingsSection(title = stringResource(R.string.about_features_title)) {
            listOf(R.string.about_feature_private_dns, R.string.about_feature_usb_debugging,
                R.string.about_feature_auto_revert, R.string.about_feature_custom_dns).forEach { Text(stringResource(it)) }
        }
        SettingsSection(title = stringResource(R.string.about_developer_title)) {
            Text(stringResource(R.string.about_developer_info))
            AppLinkButton(stringResource(R.string.about_button_github), stringResource(R.string.github_repo_url), Modifier.fillMaxWidth())
            AppLinkButton(stringResource(R.string.about_button_fdroid), stringResource(R.string.fdroid_app_url), Modifier.fillMaxWidth())
            AppButton(stringResource(R.string.about_button_permission_help), onOpenPermissionDialog,
                Modifier.fillMaxWidth(), style = AppButtonStyle.Secondary)
        }
    }
}
