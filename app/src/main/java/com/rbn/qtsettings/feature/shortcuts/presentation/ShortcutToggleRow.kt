package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsFavoriteSwitchRow

@Composable
internal fun ShortcutToggleRow(
    item: ShortcutUiItem, checked: Boolean, favorite: Boolean, enabled: Boolean,
    favoriteEnabled: Boolean, onCheckedChange: (Boolean) -> Unit,
    onFavoriteChange: (Boolean) -> Unit, onDisabledClick: () -> Unit,
    onFavoriteLimitReached: () -> Unit
) {
    val title = item.title.resolve()
    SettingsFavoriteSwitchRow(
        title = title, summary = item.subtitle.resolve(), icon = painterResource(item.iconRes),
        checked = checked, favorite = favorite, canToggle = enabled, canFavorite = favoriteEnabled,
        favoriteDescription = stringResource(if (favorite) R.string.shortcut_settings_remove_favorite_desc
            else R.string.shortcut_settings_mark_favorite_desc, title),
        onCheckedChange = onCheckedChange, onFavoriteChange = onFavoriteChange,
        onToggleLimit = onDisabledClick, onFavoriteLimit = onFavoriteLimitReached
    )
}
