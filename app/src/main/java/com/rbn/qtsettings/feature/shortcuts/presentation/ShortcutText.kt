package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

internal sealed interface ShortcutText {
    data class Resource(val id: Int) : ShortcutText
    data class Value(val text: String) : ShortcutText
}

@Composable
internal fun ShortcutText.resolve(): String = when (this) {
    is ShortcutText.Resource -> stringResource(id)
    is ShortcutText.Value -> text
}
