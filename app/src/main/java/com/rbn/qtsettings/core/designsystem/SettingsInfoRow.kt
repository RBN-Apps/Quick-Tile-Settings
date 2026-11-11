package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * A closing note for a group: no title and no action, only what the rows above need explaining.
 * It keeps the row geometry so its icon sits on the same line as theirs.
 */
@Composable
fun SettingsInfoRow(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info
) {
    Row(
        modifier.fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight)
            .padding(horizontal = AppSpacing.rowHorizontal, vertical = AppSpacing.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
