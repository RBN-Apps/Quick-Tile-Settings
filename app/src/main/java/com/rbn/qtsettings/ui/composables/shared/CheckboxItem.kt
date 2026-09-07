package com.rbn.qtsettings.ui.composables.shared

import androidx.compose.runtime.Composable
import com.rbn.qtsettings.core.designsystem.SettingsCheckboxRow

/** Compatibility entry point until the DNS/USB screen migrations (#114/#116). */
@Composable
fun CheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    enabled: Boolean = true,
    summary: String? = null
) {
    SettingsCheckboxRow(
        title = label,
        summary = summary,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled
    )
}
