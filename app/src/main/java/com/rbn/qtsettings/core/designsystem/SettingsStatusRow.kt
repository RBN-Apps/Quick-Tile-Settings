package com.rbn.qtsettings.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * Reports live system state. With [onClick] it also advances the tile cycle, which is a write to
 * a system setting, so it takes the button role and a visible affordance rather than looking like
 * a plain readout.
 */
@Composable
fun SettingsStatusRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    summary: String? = null,
    status: String? = null,
    stateLabel: String? = null,
    actionLabel: String? = null,
    reserveStatusLine: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) = StatusRow(title, icon, null, modifier, summary, status, stateLabel, actionLabel, reserveStatusLine, enabled, onClick)

@Composable
fun SettingsStatusRow(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    summary: String? = null,
    status: String? = null,
    stateLabel: String? = null,
    actionLabel: String? = null,
    reserveStatusLine: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) = StatusRow(title, null, icon, modifier, summary, status, stateLabel, actionLabel, reserveStatusLine, enabled, onClick)

@Composable
private fun StatusRow(
    title: String,
    icon: ImageVector?,
    iconPainter: Painter?,
    modifier: Modifier,
    summary: String?,
    status: String?,
    stateLabel: String?,
    actionLabel: String?,
    reserveStatusLine: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?
) {
    // One full turn per tap: the reported state may read the same after a cycle, so the
    // affordance itself has to acknowledge the tap.
    var turns by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = turns * 360f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "Status cycle rotation"
    )
    SettingsRowContent(
        title = title,
        summary = summary,
        icon = icon,
        iconPainter = iconPainter,
        enabled = enabled,
        status = status,
        reserveStatusLine = reserveStatusLine,
        modifier = modifier
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(enabled = enabled, role = Role.Button) {
                    turns += 1
                    onClick()
                }
            )
            .statusSemantics(stateLabel, actionLabel.takeIf { onClick != null }, title, status, summary),
        trailing = {
            if (onClick != null) Icon(
                imageVector = Icons.Outlined.Cached,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else AppSpacing.disabledContentAlpha
                )
            )
        }
    )
}

/** Without a visible section heading the row needs to name what it reports, and what a tap does. */
private fun Modifier.statusSemantics(
    stateLabel: String?,
    actionLabel: String?,
    vararg lines: String?
): Modifier {
    if (stateLabel == null) return this
    val spoken = buildString {
        append(stateLabel).append(": ").append(lines.filterNotNull().joinToString(", "))
        if (actionLabel != null) append(". ").append(actionLabel)
    }
    return clearAndSetSemantics {
        contentDescription = spoken
        if (actionLabel != null) role = Role.Button
    }
}
