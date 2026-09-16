package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.*
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(when {
                    operation.granting -> R.string.permission_grant_running
                    operation.checkFailed -> R.string.permission_check_failed
                    !initialized || operation.checking -> R.string.permission_checking
                    snapshot.hasWriteSecureSettings -> R.string.permission_is_granted
                    else -> R.string.permission_not_setup
                }), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.permission_page_explanation))
                feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                AppButton(stringResource(R.string.permission_refresh), onRefresh,
                    enabled = !operation.checking && !operation.granting, style = AppButtonStyle.Secondary)
            }
        }
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
