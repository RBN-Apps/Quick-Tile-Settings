package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog

@Composable
internal fun ShortcutGroup(
    title: String,
    items: List<ShortcutUiItem>,
    enabledShortcutIds: Set<String>,
    favoriteShortcutIds: Set<String>,
    maxShortcutCount: Int,
    onLimitReached: () -> Unit,
    onFavoriteLimitReached: () -> Unit,
    onToggle: (shortcutId: String, shouldEnable: Boolean) -> Unit,
    onFavorite: (shortcutId: String, shouldFavorite: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(items = items, itemKey = { it.id }, title = title, modifier = modifier) { item ->
        val isEnabled = enabledShortcutIds.contains(item.id)
        val isFavorite = favoriteShortcutIds.contains(item.id)
        val canEnableMore = enabledShortcutIds.size < maxShortcutCount
        val canFavoriteMore = favoriteShortcutIds.size < ShortcutCatalog.MAX_FAVORITE_SHORTCUTS
        ShortcutToggleRow(
            item = item,
            checked = isEnabled,
            favorite = isFavorite,
            enabled = isEnabled || canEnableMore,
            favoriteEnabled = isEnabled && (isFavorite || canFavoriteMore),
            onCheckedChange = { shouldEnable -> onToggle(item.id, shouldEnable) },
            onFavoriteChange = { shouldFavorite -> onFavorite(item.id, shouldFavorite) },
            onDisabledClick = onLimitReached,
            onFavoriteLimitReached = onFavoriteLimitReached
        )
    }
}

