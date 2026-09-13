package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.domain.orderedMethods

@Composable
internal fun PermissionMethods(
    snapshot: PermissionSnapshot,
    enabled: Boolean,
    onOpenAdb: () -> Unit,
    onRequestShizuku: () -> Unit,
    onGrantShizuku: () -> Unit,
    onGrantRoot: () -> Unit,
    showRecommendation: Boolean
) {
    val methods = snapshot.orderedMethods()
    SettingsGroup(methods, itemKey = { it }, title = stringResource(R.string.permission_methods_title)) { method ->
        Column {
            if (showRecommendation && method == methods.first()) {
                Text(stringResource(R.string.permission_recommended),
                    Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            when (method) {
                PermissionMethod.Adb -> SettingsNavigationRow(stringResource(R.string.permission_adb_title), Icons.Outlined.Computer,
                    onOpenAdb, summary = stringResource(R.string.permission_method_adb_desc),
                    status = stringResource(R.string.button_show_adb_instructions))
                PermissionMethod.Shizuku -> Column {
                    SettingsRowContent(stringResource(R.string.permission_shizuku_title),
                        stringResource(when {
                            !enabled -> R.string.permission_check_before_grant
                            !snapshot.shizukuAvailable -> R.string.shizuku_not_available_detailed
                            !snapshot.shizukuPermissionGranted -> R.string.shizuku_permission_not_granted_to_app_detailed
                            else -> R.string.shizuku_ready_to_grant_desc
                        }), null, true, Modifier,
                        iconPainter = painterResource(R.drawable.ic_shizuku)) {}
                    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            !snapshot.shizukuAvailable -> {
                                AppLinkButton(stringResource(R.string.button_open_shizuku_play_store_short),
                                    stringResource(R.string.shizuku_play_store_url), Modifier.fillMaxWidth())
                                AppLinkButton(stringResource(R.string.button_open_shizuku_github_short),
                                    stringResource(R.string.shizuku_github_releases_url), Modifier.fillMaxWidth())
                            }
                            !snapshot.shizukuPermissionGranted -> AppButton(stringResource(R.string.button_request_shizuku_permission),
                                onRequestShizuku, enabled = enabled && !snapshot.hasWriteSecureSettings)
                            else -> AppButton(stringResource(R.string.button_grant_with_shizuku),
                                onGrantShizuku, enabled = enabled && !snapshot.hasWriteSecureSettings)
                        }
                    }
                }
                PermissionMethod.Root -> Column {
                    SettingsRowContent(stringResource(R.string.permission_root_title),
                        stringResource(if (!enabled) R.string.permission_check_before_grant
                        else if (snapshot.rooted) R.string.permission_root_detected else R.string.device_not_rooted_detailed),
                        null, true, Modifier,
                        iconPainter = painterResource(R.drawable.ic_magisk)) {}
                    if (snapshot.rooted) AppButton(stringResource(R.string.button_grant_with_root), onGrantRoot,
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        enabled = enabled && !snapshot.hasWriteSecureSettings)
                }
            }
        }
    }
}
