package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.AppLinkButton
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.presentation.AdbCommandSection

/** The chosen way to the permission, with the live status the page is waiting for. */
@Composable
internal fun OnboardingGrantPage(
    state: OnboardingUiState,
    actions: OnboardingUiActions,
    modifier: Modifier = Modifier
) {
    val method = state.setupMethod
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        when (method) {
            PermissionMethod.Shizuku -> OnboardingHero(null, stringResource(R.string.permission_shizuku_title),
                stringResource(R.string.onboarding_grant_shizuku_subtitle), compact = true,
                painter = painterResource(R.drawable.ic_shizuku))
            PermissionMethod.Root -> OnboardingHero(null, stringResource(R.string.permission_root_title),
                stringResource(R.string.onboarding_grant_root_subtitle), compact = true,
                painter = painterResource(R.drawable.ic_magisk))
            else -> OnboardingHero(Icons.Outlined.Computer, stringResource(R.string.permission_adb_title),
                stringResource(R.string.permission_adb_intro), compact = true)
        }
        OnboardingStatusCard(state)
        when (method) {
            PermissionMethod.Shizuku -> ShizukuGrant(state, actions)
            PermissionMethod.Root -> RootGrant(state, actions)
            else -> AdbGrant(state, actions)
        }
    }
}

@Composable
private fun AdbGrant(state: OnboardingUiState, actions: OnboardingUiActions) {
    val steps = listOf(
        R.string.permission_adb_step_computer to R.string.permission_adb_computer_summary,
        R.string.permission_adb_step_device to R.string.permission_adb_device_summary,
        R.string.permission_adb_step_connect to R.string.permission_adb_connect_summary
    )
    SettingsGroup(steps, itemKey = { it.first }) { (title, summary) ->
        SettingsRowContent(stringResource(title), stringResource(summary), null, true, Modifier) {}
    }
    AppLinkButton(stringResource(R.string.permission_adb_platform_tools),
        stringResource(R.string.platform_tools_url), Modifier.fillMaxWidth())
    AppButton(stringResource(R.string.button_open_developer_options), actions::openDeveloperOptions,
        Modifier.fillMaxWidth(), style = AppButtonStyle.Secondary, icon = Icons.Outlined.DeveloperMode)
    AdbCommandSection(stringResource(R.string.permission_adb_step_grant),
        stringResource(R.string.help_dialog_how_to_grant_via_adb), state.grantCommand,
        stringResource(R.string.button_copy_grant_command), actions::copyCommand)
    Text(stringResource(R.string.onboarding_adb_verify), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ShizukuGrant(state: OnboardingUiState, actions: OnboardingUiActions) {
    val snapshot = state.permission
    Text(stringResource(when {
        !snapshot.shizukuAvailable -> R.string.shizuku_not_available_detailed
        !snapshot.shizukuPermissionGranted -> R.string.shizuku_permission_not_granted_to_app_detailed
        else -> R.string.shizuku_ready_to_grant_desc
    }), style = MaterialTheme.typography.bodyMedium)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
        if (!snapshot.shizukuAvailable) {
            AppLinkButton(stringResource(R.string.button_open_shizuku_play_store_short),
                stringResource(R.string.shizuku_play_store_url), Modifier.fillMaxWidth())
            AppLinkButton(stringResource(R.string.button_open_shizuku_github_short),
                stringResource(R.string.shizuku_github_releases_url), Modifier.fillMaxWidth())
        } else if (!snapshot.shizukuPermissionGranted) {
            AppButton(stringResource(R.string.button_request_shizuku_permission),
                actions::requestShizukuPermission, Modifier.fillMaxWidth(), enabled = !state.busy)
        } else {
            AppButton(stringResource(R.string.button_grant_with_shizuku), actions::grantWithShizuku,
                Modifier.fillMaxWidth(), enabled = !state.busy && !snapshot.hasWriteSecureSettings)
        }
    }
}

@Composable
private fun RootGrant(state: OnboardingUiState, actions: OnboardingUiActions) {
    Text(stringResource(if (state.permission.rooted) R.string.permission_root_detected
        else R.string.device_not_rooted_detailed), style = MaterialTheme.typography.bodyMedium)
    if (state.permission.rooted) AppButton(stringResource(R.string.button_grant_with_root),
        actions::grantWithRoot, Modifier.fillMaxWidth(),
        enabled = !state.busy && !state.permission.hasWriteSecureSettings)
}
