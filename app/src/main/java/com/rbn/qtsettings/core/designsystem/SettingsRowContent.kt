package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
internal fun SettingsRowContent(
    title: String,
    summary: String?,
    icon: ImageVector?,
    enabled: Boolean,
    modifier: Modifier,
    status: String? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val alpha = if (enabled) 1f else AppSpacing.disabledContentAlpha
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight)
            .padding(horizontal = AppSpacing.rowHorizontal, vertical = AppSpacing.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    modifier = Modifier.weight(1f, fill = false))
                // The badge marks state; it never becomes a second control.
                titleBadge?.invoke()
            }
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            }
            if (status != null) {
                Text(status, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            }
        }
        trailing()
    }
}
