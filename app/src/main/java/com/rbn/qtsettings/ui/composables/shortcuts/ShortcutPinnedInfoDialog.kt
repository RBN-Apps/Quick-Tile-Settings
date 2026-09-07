package com.rbn.qtsettings.ui.composables.shortcuts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppInfoDialog

@Composable
fun ShortcutPinnedInfoDialog(onDismissRequest: () -> Unit) {
    AppInfoDialog(
        title = stringResource(R.string.shortcut_settings_allow_pinned_when_disabled),
        message = stringResource(R.string.shortcut_settings_allow_pinned_when_disabled_desc),
        onDismissRequest = onDismissRequest
    )
}
