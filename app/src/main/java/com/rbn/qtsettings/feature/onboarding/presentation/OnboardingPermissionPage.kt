package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.domain.orderedMethods

/** Why the permission is needed, what Android currently reports, and the ways to get there. */
@Composable
internal fun OnboardingPermissionPage(
    state: OnboardingUiState,
    onChoose: (PermissionMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val methods = state.permission.orderedMethods()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        OnboardingHero(Icons.Outlined.Lock, stringResource(R.string.onboarding_permission_title),
            stringResource(R.string.onboarding_permission_subtitle), compact = true)
        OnboardingStatusCard(state)
        SettingsGroup(methods, itemKey = { it }, title = stringResource(R.string.onboarding_methods_title)) { method ->
            OnboardingMethodRow(method, state, recommended = method == methods.first(), onChoose)
        }
    }
}

/** The current answer from Android, in the same words the setup page uses. */
@Composable
internal fun OnboardingStatusCard(state: OnboardingUiState, modifier: Modifier = Modifier) {
    val granted = state.granted
    val icon = when {
        granted -> Icons.Outlined.CheckCircle
        state.operation.checkFailed -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Lock
    }
    val status = when {
        state.operation.granting -> R.string.permission_grant_running
        state.operation.checkFailed -> R.string.permission_check_failed
        !state.initialized || state.operation.checking -> R.string.permission_checking
        granted -> R.string.permission_is_granted
        else -> R.string.permission_not_setup
    }
    // The permission can arrive while the user is reading, so the card changes rather than swaps.
    val container by animateColorAsState(if (granted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest, tween(400), label = "onboarding_status_container")
    val content by animateColorAsState(if (granted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface, tween(400), label = "onboarding_status_content")
    Card(modifier.fillMaxWidth(), shape = AppShapes.groupItem(0, 1),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Row(Modifier.padding(AppSpacing.rowHorizontal), horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap),
            verticalAlignment = Alignment.CenterVertically) {
            OnboardingCrossfade(icon) { Icon(it, null, Modifier.size(AppSpacing.iconSize)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
                // The status can change while the user is at a computer, so it announces itself.
                OnboardingCrossfade(status) {
                    Text(stringResource(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.titleMedium)
                }
                state.feedback?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun OnboardingMethodRow(
    method: PermissionMethod,
    state: OnboardingUiState,
    recommended: Boolean,
    onChoose: (PermissionMethod) -> Unit
) {
    Column {
        // Above the row, as on the setup page: an inline badge squeezes the longest method name.
        if (recommended && state.initialized) Text(stringResource(R.string.permission_recommended),
            Modifier.padding(start = AppSpacing.rowHorizontal, top = 12.dp, end = AppSpacing.rowHorizontal),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        SettingsRowContent(
            title = stringResource(method.title),
            summary = stringResource(method.summary(state)),
            icon = method.vectorIcon,
            enabled = true,
            modifier = Modifier.clickable(role = Role.Button) { onChoose(method) },
            iconPainter = method.painterIcon()
        ) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val PermissionMethod.title get() = when (this) {
    PermissionMethod.Adb -> R.string.permission_adb_title
    PermissionMethod.Shizuku -> R.string.permission_shizuku_title
    PermissionMethod.Root -> R.string.permission_root_title
}

/** Only ADB has a vector icon; the other two ship as artwork of their own project. */
private val PermissionMethod.vectorIcon: ImageVector? get() =
    if (this == PermissionMethod.Adb) Icons.Outlined.Computer else null

@Composable
private fun PermissionMethod.painterIcon(): Painter? = when (this) {
    PermissionMethod.Adb -> null
    PermissionMethod.Shizuku -> painterResource(R.drawable.ic_shizuku)
    PermissionMethod.Root -> painterResource(R.drawable.ic_magisk)
}

private fun PermissionMethod.summary(state: OnboardingUiState) = when (this) {
    PermissionMethod.Adb -> R.string.onboarding_method_adb
    PermissionMethod.Shizuku -> if (state.permission.shizukuAvailable) R.string.onboarding_method_shizuku_ready
        else R.string.onboarding_method_shizuku_missing
    PermissionMethod.Root -> if (state.permission.rooted) R.string.onboarding_method_root_ready
        else R.string.onboarding_method_root_missing
}
