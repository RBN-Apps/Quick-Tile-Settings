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
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * A command to run on the computer: selectable for typing along, copyable for pasting. Emitted
 * as two stacked elements so it can sit inside the step card that asks for the command.
 */
@Composable
internal fun AdbCommandBlock(command: String, copyLabel: String, onCopy: (String) -> Unit) {
    // The block always sits inside a card now, so it steps away from the card's own container
    // colour rather than towards it.
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.medium) {
        SelectionContainer {
            Text(command, Modifier.fillMaxWidth().padding(AppSpacing.rowHorizontal),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        }
    }
    AppButton(copyLabel, { onCopy(command) }, Modifier.fillMaxWidth(),
        style = AppButtonStyle.Secondary, icon = Icons.Outlined.ContentCopy)
}
