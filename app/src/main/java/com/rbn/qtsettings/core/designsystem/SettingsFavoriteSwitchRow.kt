package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/** The row owns the switch action; the independently accessible star owns favorite selection. */
@Composable
fun SettingsFavoriteSwitchRow(
    title: String, summary: String, icon: Painter, checked: Boolean, favorite: Boolean,
    canToggle: Boolean, canFavorite: Boolean, favoriteDescription: String,
    onCheckedChange: (Boolean) -> Unit, onFavoriteChange: (Boolean) -> Unit,
    onToggleLimit: () -> Unit, onFavoriteLimit: () -> Unit, modifier: Modifier = Modifier
) {
    SettingsRowContent(title, summary, icon = null, enabled = canToggle,
        iconPainter = icon,
        modifier = modifier.toggleable(checked, role = Role.Switch) {
            if (canToggle) onCheckedChange(it) else onToggleLimit()
        }
    ) {
        IconButton(enabled = checked, onClick = {
            if (canFavorite) onFavoriteChange(!favorite) else onFavoriteLimit()
        }) {
            Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, favoriteDescription,
                tint = (if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    .copy(alpha = if (checked && canFavorite) 1f else AppSpacing.disabledContentAlpha))
        }
        // Keep limit explanations reachable through the row, without a second switch click handler.
        Switch(checked, onCheckedChange = null,
            modifier = Modifier.alpha(if (canToggle) 1f else AppSpacing.disabledContentAlpha))
    }
}
