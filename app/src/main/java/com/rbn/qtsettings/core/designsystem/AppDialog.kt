package com.rbn.qtsettings.core.designsystem

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.DialogProperties
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Shared modal frame. The owner decides when to remove it and what confirmation does.
 * Content scrolls independently of the actions; do not nest another vertical scroller here.
 * Android/Material own outside-touch dismissal and IME window resizing.
 * With decor fitting enabled, Android also owns the insets; do not apply them a second time.
 * Back stays a dismissal; [DialogBackPreview] only previews it while the gesture runs.
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
    val backScale = remember { Animatable(1f) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = (if (properties.decorFitsSystemWindows) modifier else modifier.safeDrawingPadding())
            .graphicsLayer { scaleX = backScale.value; scaleY = backScale.value },
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
                // AlertDialog composes its slots inside the dialog's own window, which is the only
                // place where the back dispatcher is the dialog's rather than the Activity's.
                DialogBackPreview(backScale, properties.dismissOnBackPress, onDismissRequest)
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

/** Material's predictive-back curve, matching the previews Android draws for its own surfaces. */
private val DialogBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

/** The frame only draws smaller; its layout size stays put so the content never reflows. */
private const val DialogBackMinScale = 0.9f

/**
 * Compose keeps an always-enabled dismiss callback on every dialog window, so SystemUI has nothing
 * left to animate and dialogs are the one back owner in the app without a predictive preview.
 * Registering a handler in front of that callback restores it: [scale] follows the gesture, only a
 * completed gesture dismisses, and a cancelled one settles back without touching the owner.
 * Emits nothing; it must be called from inside the dialog window to reach the right dispatcher.
 */
@Composable
private fun DialogBackPreview(scale: Animatable<Float, *>, enabled: Boolean, onDismissRequest: () -> Unit) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    // Cancelling the gesture cancels the handler's own coroutine, so settling needs a second scope.
    val settle = rememberCoroutineScope()
    PredictiveBackHandler(enabled) { progress ->
        try {
            progress.collect { event ->
                scale.snapTo(lerp(1f, DialogBackMinScale, DialogBackEasing.transform(event.progress)))
            }
            dismiss()
            // Owners decide when the dialog goes away. Surviving this frame means it stayed open,
            // so release the preview instead of leaving the frame permanently shrunk.
            withFrameNanos { }
            scale.snapTo(1f)
        } catch (cancelled: CancellationException) {
            settle.launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
            throw cancelled
        }
    }
}
