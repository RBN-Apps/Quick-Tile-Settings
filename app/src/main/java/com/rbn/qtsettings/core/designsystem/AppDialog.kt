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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.DialogProperties
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * Shared modal frame. The owner decides when to remove it and what confirmation does.
 * Content scrolls independently of the actions; do not nest another vertical scroller here.
 * Android/Material own Back, outside-touch dismissal and IME window resizing.
 * With decor fitting enabled, Android also owns the insets; do not apply them a second time.
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
    properties: DialogProperties = DialogProperties(decorFitsSystemWindows = false),
    content: @Composable ColumnScope.() -> Unit
) {
    // Android dialogs use a separate window and rebuild LocalDensity from that window's
    // configuration. Preserve an explicit caller override for previews and isolated a11y tests.
    val callerDensity = LocalDensity.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = if (properties.decorFitsSystemWindows) modifier else modifier.safeDrawingPadding(),
        properties = properties,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = icon?.let { { DialogSlot(callerDensity) { Icon(it, contentDescription = null) } } },
        title = {
            DialogSlot(callerDensity) {
                Text(title, Modifier.semantics { heading() })
            }
        },
        text = {
            DialogSlot(callerDensity) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap),
                    content = content
                )
            }
        },
        confirmButton = {
            DialogSlot(callerDensity) {
                AppButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    style = if (destructive) AppButtonStyle.Destructive else AppButtonStyle.Text
                )
            }
        },
        dismissButton = dismissLabel?.let { label ->
            {
                DialogSlot(callerDensity) {
                    AppButton(label, onDismissRequest, style = AppButtonStyle.Text)
                }
            }
        }
    )
}

@Composable
private fun DialogSlot(density: Density, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides density, content = content)
}
