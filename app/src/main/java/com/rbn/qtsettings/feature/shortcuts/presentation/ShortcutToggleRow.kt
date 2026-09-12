package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R

@Composable
internal fun ShortcutToggleRow(
    item: ShortcutUiItem,
    checked: Boolean,
    favorite: Boolean,
    enabled: Boolean,
    favoriteEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onDisabledClick: () -> Unit,
    onFavoriteLimitReached: () -> Unit
) {
    val title = item.title.resolve()
    val subtitle = item.subtitle.resolve()
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (enabled) {
                        onCheckedChange(!checked)
                    } else {
                        onDisabledClick()
                    }
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
        IconButton(
            enabled = checked,
            onClick = {
                if (favoriteEnabled) {
                    onFavoriteChange(!favorite)
                } else {
                    onFavoriteLimitReached()
                }
            },
            modifier = Modifier.alpha(if (checked && favoriteEnabled) 1f else 0.5f)
        ) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (favorite) {
                    stringResource(R.string.shortcut_settings_remove_favorite_desc, title)
                } else {
                    stringResource(R.string.shortcut_settings_mark_favorite_desc, title)
                },
                tint = if (favorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Switch(
            checked = checked,
            enabled = true,
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            onCheckedChange = { shouldEnable ->
                if (enabled) {
                    onCheckedChange(shouldEnable)
                } else {
                    onDisabledClick()
                }
            }
        )
    }
}

