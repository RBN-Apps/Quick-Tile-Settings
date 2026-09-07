package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
fun SettingsNavigationRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    status: String? = null,
    enabled: Boolean = true
) {
    SettingsRowContent(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        status = status,
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else AppSpacing.disabledContentAlpha
            )
        )
    }
}
