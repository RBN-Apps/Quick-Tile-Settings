package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppInfoDialog
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutPinnedInfoDialog

@Composable
internal fun ShortcutSettingsDialogs(state: ShortcutSettingsState, onAction: (ShortcutSettingsAction) -> Unit) {
    val dismiss = { onAction(ShortcutSettingsAction.DismissDialog) }
    when (state.dialog) {
        ShortcutDialog.None -> Unit
        ShortcutDialog.Limit -> AppInfoDialog(
            stringResource(R.string.shortcut_settings_limit_reached_title),
            stringResource(R.string.shortcut_settings_limit_reached), dismiss)
        ShortcutDialog.FavoriteLimit -> AppInfoDialog(
            stringResource(R.string.shortcut_settings_favorite_limit_reached_title),
            stringResource(R.string.shortcut_settings_favorite_limit_reached), dismiss)
        ShortcutDialog.PinnedInfo -> ShortcutPinnedInfoDialog(dismiss)
        ShortcutDialog.PinnedWarning -> AppConfirmationDialog(
            title = stringResource(R.string.shortcut_settings_allow_pinned_warning_title),
            message = stringResource(R.string.shortcut_settings_allow_pinned_warning_message),
            confirmLabel = stringResource(R.string.shortcut_settings_allow_pinned_warning_confirm),
            onConfirm = { onAction(ShortcutSettingsAction.SetPinnedFallback(true)); dismiss() },
            onDismissRequest = dismiss)
    }
}
