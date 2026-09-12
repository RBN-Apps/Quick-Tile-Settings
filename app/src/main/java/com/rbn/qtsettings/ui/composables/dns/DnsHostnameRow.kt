package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry

internal val DnsHostnameRowHeight = 76.dp

@Composable
fun DnsHostnameRow(
    entry: DnsHostnameEntry,
    isActive: Boolean,
    isDragged: Boolean,
    reorderEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onSetActiveClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMeasured: (Int) -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onDragCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    canActivate: Boolean = true
) {
    var rowCenterY by remember(entry.id) { mutableFloatStateOf(0f) }
    val moveUpLabel = stringResource(R.string.dns_move_up)
    val moveDownLabel = stringResource(R.string.dns_move_down)
    val reorderDescription = stringResource(
        R.string.dns_reorder_entry,
        entry.name,
        entry.hostname
    )
    val dragHandleColor by animateColorAsState(
        targetValue = if (isDragged) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 120),
        label = "dnsDragHandle"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DnsHostnameRowHeight)
            .onSizeChanged { size -> onMeasured(size.height) }
            .onGloballyPositioned { coordinates ->
                rowCenterY = coordinates.boundsInWindow().center.y
            }

    ) {
        Row(
            Modifier.weight(1f).toggleable(value = entry.isSelectedForCycle, role = Role.Checkbox,
                onValueChange = onSelectionChanged).padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (entry.isPredefined) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.dns_active),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!entry.isPredefined) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.12f
                            ),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.dns_custom_entry_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = entry.hostname,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Checkbox(checked = entry.isSelectedForCycle, onCheckedChange = null)
        }

        if (reorderEnabled) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                tint = dragHandleColor,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = reorderDescription
                        customActions = buildList {
                            if (canMoveUp) {
                                add(CustomAccessibilityAction(moveUpLabel) {
                                    onMoveUp()
                                    true
                                })
                            }
                            if (canMoveDown) {
                                add(CustomAccessibilityAction(moveDownLabel) {
                                    onMoveDown()
                                    true
                                })
                            }
                        }
                    }
                    .pointerInput(entry.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(rowCenterY) },
                            onDragEnd = onDragStopped,
                            onDragCancel = onDragCancelled,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    }
                    .padding(12.dp)
            )
        }

        DnsHostnameActionsMenu(entry, isActive, onSetActiveClicked, onInfoClicked, onEditClicked, onDeleteClicked, canActivate)
    }
}
