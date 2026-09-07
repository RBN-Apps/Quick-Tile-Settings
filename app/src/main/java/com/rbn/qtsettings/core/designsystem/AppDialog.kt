package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * Shared modal frame. The owner decides when to remove it and what confirmation does.
 * Content scrolls independently of the actions; do not nest another vertical scroller here.
 * Android/Material own Back, outside-touch dismissal and IME window resizing.
 */
@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.safeDrawingPadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title, Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap),
                content = content
            )
        },
        confirmButton = {
            AppButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = confirmEnabled,
                style = if (destructive) AppButtonStyle.Destructive else AppButtonStyle.Text
            )
        },
        dismissButton = dismissLabel?.let { label ->
            {
                AppButton(label, onDismissRequest, style = AppButtonStyle.Text)
            }
        }
    )
}
