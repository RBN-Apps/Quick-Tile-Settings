package com.rbn.qtsettings.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

// Fade and shrink must land on the exact same frame. Two springs do not: they run on different
// value types and settle at different times, which reads as the title snapping into place once
// the text is already gone. One shared tween keeps the whole line moving as a single object.
private const val STATUS_LINE_MILLIS = 300
private val STATUS_LINE_SIZE = tween<androidx.compose.ui.unit.IntSize>(STATUS_LINE_MILLIS, easing = FastOutSlowInEasing)
private val STATUS_LINE_FADE = tween<Float>(STATUS_LINE_MILLIS, easing = FastOutSlowInEasing)

@Composable
internal fun SettingsRowContent(
    title: String,
    summary: String?,
    icon: ImageVector?,
    enabled: Boolean,
    modifier: Modifier,
    status: String? = null,
    iconPainter: Painter? = null,
    // A row whose icon carries the state itself (a granted check) needs to leave the neutral tint.
    iconTint: Color? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    // Reported state comes and goes with its second line. Reserving that line keeps the row
    // from resizing, and lets the title sit centred whenever the line is absent.
    reserveStatusLine: Boolean = false,
    trailing: @Composable () -> Unit
) {
    val alpha = if (enabled) 1f else AppSpacing.disabledContentAlpha
    val resolvedIconTint = (iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha)
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight)
            .padding(horizontal = AppSpacing.rowHorizontal, vertical = AppSpacing.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSize),
                tint = resolvedIconTint)
        }
        if (icon == null && iconPainter != null) {
            Icon(iconPainter, contentDescription = null, modifier = Modifier.size(AppSpacing.iconSize),
                tint = resolvedIconTint)
        }
        // Derived from the type scale rather than a fixed dp, so it still holds at large font sizes.
        val reservedHeight = with(LocalDensity.current) {
            MaterialTheme.typography.bodyLarge.lineHeight.toDp() + AppSpacing.textGap +
                MaterialTheme.typography.bodyMedium.lineHeight.toDp()
        }
        // The exit animation still needs the text it is fading out.
        var lastStatus by remember { mutableStateOf(status) }
        if (status != null) lastStatus = status
        Box(
            Modifier.weight(1f).then(if (reserveStatusLine) Modifier.heightIn(min = reservedHeight) else Modifier),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                // The gap has to travel inside the animated line. As Arrangement spacing it
                // would vanish in a single frame once the line leaves the composition, jolting
                // the centred title right at the end of the exit.
                verticalArrangement =
                    if (reserveStatusLine) Arrangement.Top else Arrangement.spacedBy(AppSpacing.textGap)
            ) {
                val ownGap = if (reserveStatusLine) Modifier.padding(top = AppSpacing.textGap) else Modifier
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        modifier = Modifier.weight(1f, fill = false))
                    // The badge marks state; it never becomes a second control.
                    titleBadge?.invoke()
                }
                if (summary != null) {
                    Text(summary, modifier = ownGap, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                }
                if (reserveStatusLine) {
                    AnimatedVisibility(
                        visible = status != null,
                        enter = fadeIn(STATUS_LINE_FADE) + expandVertically(STATUS_LINE_SIZE),
                        exit = fadeOut(STATUS_LINE_FADE) + shrinkVertically(STATUS_LINE_SIZE)
                    ) {
                        Text(lastStatus.orEmpty(), modifier = Modifier.padding(top = AppSpacing.textGap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    }
                } else if (status != null) {
                    Text(status, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                }
            }
        }
        trailing()
    }
}
