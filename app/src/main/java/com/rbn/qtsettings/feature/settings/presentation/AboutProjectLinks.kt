package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSection

@Composable
internal fun AboutProjectLinks(onPermissionHelp: () -> Unit, onLink: (String) -> Unit, onReport: () -> Unit, onLicenses: () -> Unit) {
    val github = stringResource(R.string.github_repo_url)
    val fdroid = stringResource(R.string.fdroid_app_url)
    val links = listOf<Pair<Int, () -> Unit>>(
        R.string.about_button_github to { onLink(github) },
        R.string.about_button_fdroid to { onLink(fdroid) },
        R.string.about_releases to { onLink("$github/releases") },
        R.string.about_report_issue to onReport,
        R.string.about_feature_request to { onLink(com.rbn.qtsettings.platform.app.featureRequestUrl(github)) },
        R.string.about_license to { onLink("$github/blob/HEAD/LICENSE") },
        R.string.about_third_party_licenses to onLicenses
    )
    SettingsSection(title = stringResource(R.string.about_project_title)) {
        Text(stringResource(R.string.about_developer_info))
        SettingsGroup(items = links, itemKey = { it.first }) { (title, onClick) ->
            // The only entry that stays in the app, so it is the only one without the external-link icon.
            val inApp = title == R.string.about_third_party_licenses
            SettingsNavigationRow(stringResource(title),
                if (inApp) Icons.Outlined.Description else Icons.AutoMirrored.Outlined.OpenInNew, onClick)
        }
        SettingsGroup(items = listOf(Unit), itemKey = { "permission" }) {
            SettingsNavigationRow(stringResource(R.string.about_permission_setup),
                Icons.Outlined.AdminPanelSettings, onPermissionHelp,
                summary = stringResource(R.string.about_permission_summary))
        }
    }
}
