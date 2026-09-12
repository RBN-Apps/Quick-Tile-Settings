package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.SettingsSection
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

@Composable
internal fun AdbCommandSection(title: String, description: String, command: String,
    copyLabel: String, onCopy: (String) -> Unit) {
    SettingsSection(title = title) {
        Text(description)
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            SelectionContainer {
                Text(command, Modifier.fillMaxWidth().padding(AppSpacing.rowHorizontal),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
        }
        AppButton(copyLabel, { onCopy(command) }, Modifier.fillMaxWidth(),
            style = AppButtonStyle.Secondary, icon = Icons.Outlined.ContentCopy)
    }
}
