package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutPinnedInfoDialog

@Composable
internal fun ShortcutSettingsDialogs(state: ShortcutSettingsState, onAction: (ShortcutSettingsAction) -> Unit) {
    val dismissShortcutLimitDialog = { onAction(ShortcutSettingsAction.DismissDialog) }
    val dismissFavoriteLimitDialog = dismissShortcutLimitDialog
    if (state.dialog == ShortcutDialog.Limit) {
        AlertDialog(
            onDismissRequest = dismissShortcutLimitDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shortcut_settings_limit_reached_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_limit_reached)) },
            confirmButton = {
                TextButton(onClick = dismissShortcutLimitDialog) {
                    Text(text = stringResource(R.string.dialog_close))
                }
            }
        )
    }

    if (state.dialog == ShortcutDialog.FavoriteLimit) {
        AlertDialog(
            onDismissRequest = dismissFavoriteLimitDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shortcut_settings_favorite_limit_reached_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_favorite_limit_reached)) },
            confirmButton = {
                TextButton(onClick = dismissFavoriteLimitDialog) {
                    Text(text = stringResource(R.string.dialog_close))
                }
            }
        )
    }

    if (state.dialog == ShortcutDialog.PinnedInfo) {
        ShortcutPinnedInfoDialog(
            onDismissRequest = { onAction(ShortcutSettingsAction.DismissDialog) }
        )
    }

    if (state.dialog == ShortcutDialog.PinnedWarning) {
        AlertDialog(
            onDismissRequest = { onAction(ShortcutSettingsAction.DismissDialog) },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(ShortcutSettingsAction.SetPinnedFallback(true))
                        onAction(ShortcutSettingsAction.DismissDialog)
                    }
                ) {
                    Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ShortcutSettingsAction.DismissDialog) }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

