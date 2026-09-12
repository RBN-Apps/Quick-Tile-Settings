package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

@Composable
internal fun ShortcutSettingsScreen(
    state: ShortcutSettingsState,
    onAction: (ShortcutSettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledShortcutIds = state.configuration.enabledIds
    val favoriteShortcutIds = state.configuration.favoriteIds
    val allowPinnedShortcutsWhenDisabled = state.configuration.allowPinnedWhenDisabled
    val shortcutMaxCount = state.configuration.maxCount
    val privateDnsItems = state.dnsItems
    val usbItems = state.usbItems
    val showShortcutLimitReachedDialogAction = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.Limit)) }
    val showFavoriteLimitReachedDialogAction = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.FavoriteLimit)) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = { onAction(ShortcutSettingsAction.ToggleInfo) }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.shortcut_settings_limit,
                            enabledShortcutIds.size,
                            shortcutMaxCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (state.infoExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                AnimatedVisibility(visible = state.infoExpanded) {
                    Text(
                        text = stringResource(
                            R.string.shortcut_settings_description,
                            shortcutMaxCount,
                            ShortcutCatalog.MAX_FAVORITE_SHORTCUTS
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            ShortcutPinnedFallbackRow(
                checked = allowPinnedShortcutsWhenDisabled,
                onCheckedChange = { shouldEnable ->
                    if (shouldEnable) {
                        onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.PinnedWarning))
                    } else {
                        onAction(ShortcutSettingsAction.SetPinnedFallback(false))
                    }
                },
                onInfoClick = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.PinnedInfo)) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        ShortcutGroup(
            title = stringResource(R.string.setting_title_private_dns),
            items = privateDnsItems,
            enabledShortcutIds = enabledShortcutIds,
            favoriteShortcutIds = favoriteShortcutIds,
            maxShortcutCount = shortcutMaxCount,
            onLimitReached = showShortcutLimitReachedDialogAction,
            onFavoriteLimitReached = showFavoriteLimitReachedDialogAction,
            onToggle = { shortcutId, shouldEnable ->
                onAction(ShortcutSettingsAction.Toggle(shortcutId, shouldEnable))
            },
            onFavorite = { shortcutId, shouldFavorite ->
                onAction(ShortcutSettingsAction.Favorite(shortcutId, shouldFavorite))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        ShortcutGroup(
            title = stringResource(R.string.setting_title_usb_debugging),
            items = usbItems,
            enabledShortcutIds = enabledShortcutIds,
            favoriteShortcutIds = favoriteShortcutIds,
            maxShortcutCount = shortcutMaxCount,
            onLimitReached = showShortcutLimitReachedDialogAction,
            onFavoriteLimitReached = showFavoriteLimitReachedDialogAction,
            onToggle = { shortcutId, shouldEnable ->
                onAction(ShortcutSettingsAction.Toggle(shortcutId, shouldEnable))
            },
            onFavorite = { shortcutId, shouldFavorite ->
                onAction(ShortcutSettingsAction.Favorite(shortcutId, shouldFavorite))
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    ShortcutSettingsDialogs(state, onAction)
}
