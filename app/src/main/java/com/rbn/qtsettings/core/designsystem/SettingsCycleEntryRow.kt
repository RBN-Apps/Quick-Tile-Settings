package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * One entry of a tile cycle: the checkbox selects it for the cycle, the badge marks the entry the
 * system currently reports as active, and the action at the end applies that entry right away.
 * Both belong to the same entry, so a feature with many entries — DNS — keeps the same row instead
 * of growing a separate block of actions.
 */
@Composable
fun SettingsCycleEntryRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isActive: Boolean,
    activeLabel: String,
    activateLabel: String,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    activateEnabled: Boolean = true
) {
    // Keep the trailing action separate while drawing the toggle feedback across the whole card.
    val toggleInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .indication(toggleInteractionSource, LocalIndication.current),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowContent(
            title = title,
            summary = summary,
            icon = icon,
            enabled = enabled,
            modifier = Modifier.weight(1f).toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = toggleInteractionSource,
                indication = null,
                onValueChange = onCheckedChange
            ),
            titleBadge = if (isActive) {
                {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = activeLabel,
                        tint = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (enabled) 1f else AppSpacing.disabledContentAlpha
                        ),
                        modifier = Modifier.size(AppSpacing.badgeSize)
                    )
                }
            } else null
        ) {
            // One interaction owner for touch, keyboard and accessibility.
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        }
        // A sibling of the toggle area, so the immediate action stays its own focus target.
        IconButton(onClick = onActivate, enabled = activateEnabled && !isActive) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = activateLabel)
        }
    }
}
