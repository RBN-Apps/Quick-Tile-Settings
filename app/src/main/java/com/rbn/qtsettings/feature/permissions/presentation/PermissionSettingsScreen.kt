package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot

/** Setup page inside the app shell; system actions are callbacks only.
 * The first run has its own pages and shares the smaller parts below. */
@Composable
fun PermissionSettingsScreen(
    snapshot: PermissionSnapshot,
    initialized: Boolean,
    operation: PermissionOperation,
    feedback: String?,
    onRefresh: () -> Unit,
    onOpenAdb: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onGrantShizuku: () -> Unit,
    onGrantRoot: () -> Unit,
    onOpenAutomation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unavailable = !initialized || operation.checkFailed || operation.checking || operation.granting
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpacing.rowHorizontal),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        PermissionStatusSection(snapshot, initialized, operation, feedback, onRefresh)
        PermissionMethods(snapshot, enabled = !unavailable,
            onOpenAdb, onRequestShizukuPermission, onGrantShizuku, onGrantRoot,
            showRecommendation = initialized && !operation.checkFailed)
        SettingsGroup(listOf(Unit), itemKey = { "developer" }) {
            SettingsNavigationRow(stringResource(R.string.button_open_developer_options),
                Icons.Outlined.DeveloperMode, onOpenDeveloperOptions,
                status = stringResource(if (operation.checkFailed) R.string.permission_check_failed
                    else if (!initialized || operation.checking) R.string.permission_checking
                    else if (snapshot.developerOptionsEnabled) R.string.developer_options_enabled_status
                    else R.string.developer_options_disabled_status))
        }
        SettingsGroup(listOf(Unit), itemKey = { "optional" }, title = stringResource(R.string.permission_optional_title)) {
            SettingsNavigationRow(stringResource(R.string.dns_menu_automation), Icons.Outlined.Autorenew,
                onOpenAutomation, summary = stringResource(R.string.permission_optional_summary))
        }
    }
}

/** The same grouped row the tile pages use to report system state: the icon carries the verdict,
 * and the row itself is the "check again" action. */
@Composable
private fun PermissionStatusSection(
    snapshot: PermissionSnapshot,
    initialized: Boolean,
    operation: PermissionOperation,
    feedback: String?,
    onRefresh: () -> Unit
) {
    val granted = snapshot.hasWriteSecureSettings
    val icon = when {
        granted -> Icons.Outlined.CheckCircle
        operation.checkFailed -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Lock
    }
    // The permission usually arrives from a computer while this page stays open, so the check
    // fades in instead of appearing between two frames.
    val tint by animateColorAsState(
        when {
            granted -> MaterialTheme.colorScheme.primary
            operation.checkFailed -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        tween(400), label = "permission_status_tint"
    )
    // Why the permission is needed belongs to the state it explains, so it hangs off the same
    // group instead of floating between two of them.
    SettingsGroup(listOf("status", "explanation"), itemKey = { it }) { item ->
        when (item) {
            "status" -> SettingsStatusRow(
                title = stringResource(when {
                    operation.granting -> R.string.permission_grant_running
                    operation.checkFailed -> R.string.permission_check_failed
                    !initialized || operation.checking -> R.string.permission_checking
                    granted -> R.string.permission_is_granted
                    else -> R.string.permission_not_setup
                }),
                icon = icon,
                status = feedback,
                stateLabel = stringResource(R.string.status_current_state),
                actionLabel = stringResource(R.string.permission_status_action),
                iconTint = tint,
                enabled = !operation.checking && !operation.granting,
                onClick = onRefresh
            )
            else -> SettingsInfoRow(stringResource(R.string.permission_page_explanation))
        }
    }
}
