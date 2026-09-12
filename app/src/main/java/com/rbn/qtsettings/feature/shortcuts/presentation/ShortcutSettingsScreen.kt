package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

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
            .verticalScroll(rememberScrollState()).padding(vertical = AppSpacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)
    ) {
        ShortcutCountInfo(state, onAction)
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
    }

    ShortcutSettingsDialogs(state, onAction)
}
