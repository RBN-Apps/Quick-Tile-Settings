package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSection

@Composable
internal fun AboutProjectLinks(onPermissionHelp: () -> Unit, onLink: (String) -> Unit, onReport: () -> Unit) {
    val github = stringResource(R.string.github_repo_url)
    val fdroid = stringResource(R.string.fdroid_app_url)
    val links = listOf(
        R.string.about_button_github to github,
        R.string.about_button_fdroid to fdroid,
        R.string.about_releases to "$github/releases",
        R.string.about_report_issue to "$github/issues/new?template=bug-report.yml",
        R.string.about_feature_request to com.rbn.qtsettings.platform.app.featureRequestUrl(github),
        R.string.about_license to "$github/blob/HEAD/LICENSE"
    )
    SettingsSection(title = stringResource(R.string.about_project_title)) {
        Text(stringResource(R.string.about_developer_info))
        Text(stringResource(R.string.about_credits))
        SettingsGroup(items = links, itemKey = { it.first }) { (title, url) ->
            SettingsNavigationRow(stringResource(title), Icons.AutoMirrored.Outlined.OpenInNew,
                { if (title == R.string.about_report_issue) onReport() else onLink(url) })
        }
        SettingsGroup(items = listOf(Unit), itemKey = { "permission" }) {
            SettingsNavigationRow(stringResource(R.string.about_permission_setup),
                Icons.Outlined.AdminPanelSettings, onPermissionHelp,
                summary = stringResource(R.string.about_permission_summary))
        }
    }
}
