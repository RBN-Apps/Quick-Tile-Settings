package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
internal fun ShortcutSettingsScreen(
    state: ShortcutSettingsState,
    onAction: (ShortcutSettingsAction) -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    initialSection: ShortcutSection? = null
) {
    // Only the first visit uses the hint. The overview (counts and the explanation entry)
    // occupies index 0 and the pinned fallback index 1; LazyListState restores the user's position thereafter.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = when (initialSection) {
        ShortcutSection.Dns -> 2
        ShortcutSection.Usb -> 3
        null -> 0
    })
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap),
        // Leave room for the entry outline above the first visible section.
        contentPadding = PaddingValues(top = AppSpacing.sectionGap, bottom = AppSpacing.sectionGap)
    ) {
        item(key = "count") {
            ShortcutOverviewGroup(state, onOpenHelp)
        }
        item(key = "pinned") {
            ShortcutPinnedFallbackRow(
                checked = state.configuration.allowPinnedWhenDisabled,
                onCheckedChange = { shouldEnable ->
                    onAction(if (shouldEnable) ShortcutSettingsAction.ShowDialog(ShortcutDialog.PinnedWarning)
                        else ShortcutSettingsAction.SetPinnedFallback(false))
                },
                onInfoClick = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.PinnedInfo)) }
            )
        }
        ShortcutSection.entries.forEach { section ->
            item(key = section.name) {
                // The last, short section can align with the top of the viewport too.
                Box(if (section == ShortcutSection.Usb) Modifier.fillParentMaxHeight() else Modifier) {
                    ShortcutSectionHighlight(initialSection == section) { highlight ->
                        ShortcutGroup(
                            modifier = highlight,
                            title = stringResource(if (section == ShortcutSection.Dns)
                                R.string.setting_title_private_dns else R.string.setting_title_usb_debugging),
                            items = if (section == ShortcutSection.Dns) state.dnsItems else state.usbItems,
                            enabledShortcutIds = state.configuration.enabledIds,
                            favoriteShortcutIds = state.configuration.favoriteIds,
                            maxShortcutCount = state.configuration.maxCount,
                            onLimitReached = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.Limit)) },
                            onFavoriteLimitReached = { onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.FavoriteLimit)) },
                            onToggle = { id, enabled -> onAction(ShortcutSettingsAction.Toggle(id, enabled)) },
                            onFavorite = { id, enabled -> onAction(ShortcutSettingsAction.Favorite(id, enabled)) }
                        )
                    }
                }
            }
        }
    }
    ShortcutSettingsDialogs(state, onAction)
}
