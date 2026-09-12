package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role

/** One accessible interaction owner for a single-choice setting. */
@Composable
fun SettingsChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    SettingsRowContent(title, summary, icon, enabled,
        modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)) {
        RadioButton(selected, onClick = null, enabled = enabled)
    }
}
