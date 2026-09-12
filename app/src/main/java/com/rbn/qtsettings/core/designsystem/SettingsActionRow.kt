package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role

@Composable
fun SettingsActionRow(title: String, summary: String?, icon: Painter, onClick: () -> Unit,
                      modifier: Modifier = Modifier) {
    SettingsRowContent(title, summary, icon = null, enabled = true, iconPainter = icon,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick), trailing = {})
}
