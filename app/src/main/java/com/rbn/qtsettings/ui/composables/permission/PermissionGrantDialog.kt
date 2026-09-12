package com.rbn.qtsettings.ui.composables.permission

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*

private enum class PermissionMethod { Adb, Shizuku, Root }

@Composable
fun PermissionGrantDialog(
    onDismissRequest: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onShowAdbInstructions: () -> Unit,
    onGrantWithShizuku: () -> Unit,
    onGrantWithRoot: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    isShizukuAvailable: Boolean,
    appHasShizukuPermission: Boolean,
    isDeviceRooted: Boolean,
    developerOptionsEnabled: Boolean = false
) {
    val methods = when {
        isDeviceRooted && isShizukuAvailable -> listOf(PermissionMethod.Root, PermissionMethod.Shizuku, PermissionMethod.Adb)
        isDeviceRooted -> listOf(PermissionMethod.Root, PermissionMethod.Adb, PermissionMethod.Shizuku)
        isShizukuAvailable -> listOf(PermissionMethod.Shizuku, PermissionMethod.Adb, PermissionMethod.Root)
        else -> listOf(PermissionMethod.Adb, PermissionMethod.Shizuku, PermissionMethod.Root)
    }
    AppDialog(stringResource(R.string.permission_grant_dialog_title), onDismissRequest,
        stringResource(R.string.dialog_close), onDismissRequest) {
        Text(stringResource(R.string.permission_grant_dialog_intro))
        methods.forEachIndexed { index, method ->
            val recommendation = if (index == 0) stringResource(R.string.recommended_for_you) else ""
            when (method) {
                PermissionMethod.Adb -> PermissionMethodSection(
                    stringResource(R.string.permission_method_adb_title, index + 1, recommendation),
                    stringResource(R.string.permission_method_adb_desc)) {
                    AppButton(stringResource(R.string.button_show_adb_instructions), onShowAdbInstructions,
                        Modifier.fillMaxWidth())
                }
                PermissionMethod.Shizuku -> PermissionMethodSection(
                    stringResource(R.string.permission_method_shizuku_title, index + 1, recommendation),
                    stringResource(when {
                        !isShizukuAvailable -> R.string.shizuku_not_available_detailed
                        !appHasShizukuPermission -> R.string.shizuku_permission_not_granted_to_app_detailed
                        else -> R.string.shizuku_ready_to_grant_desc
                    })) {
                    when {
                        !isShizukuAvailable -> {
                            AppLinkButton(stringResource(R.string.button_open_shizuku_play_store_short),
                                stringResource(R.string.shizuku_play_store_url), Modifier.fillMaxWidth())
                            AppLinkButton(stringResource(R.string.button_open_shizuku_github_short),
                                stringResource(R.string.shizuku_github_releases_url), Modifier.fillMaxWidth())
                        }
                        !appHasShizukuPermission -> AppButton(stringResource(R.string.button_request_shizuku_permission),
                            onRequestShizukuPermission, Modifier.fillMaxWidth())
                        else -> AppButton(stringResource(R.string.button_grant_with_shizuku),
                            onGrantWithShizuku, Modifier.fillMaxWidth())
                    }
                }
                PermissionMethod.Root -> PermissionMethodSection(
                    stringResource(R.string.permission_method_root_title, index + 1, recommendation),
                    stringResource(if (isDeviceRooted) R.string.root_ready_to_grant_desc else R.string.device_not_rooted_detailed)) {
                    if (isDeviceRooted) AppButton(stringResource(R.string.button_grant_with_root),
                        onGrantWithRoot, Modifier.fillMaxWidth())
                }
            }
        }
        SettingsGroup(listOf(Unit), itemKey = { "developer" }) {
            SettingsNavigationRow(stringResource(R.string.button_open_developer_options), Icons.Outlined.DeveloperMode,
                onOpenDeveloperOptions, status = stringResource(if (developerOptionsEnabled)
                    R.string.developer_options_enabled_status else R.string.developer_options_disabled_status))
        }
    }
}
