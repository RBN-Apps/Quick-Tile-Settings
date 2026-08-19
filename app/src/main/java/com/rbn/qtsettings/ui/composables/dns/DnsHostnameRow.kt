package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry

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
    modifier: Modifier = Modifier
) {
    var actionsExpanded by remember(entry.id) { mutableStateOf(false) }
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
            .clickable { onSelectionChanged(!entry.isSelectedForCycle) }
    ) {
        Checkbox(
            checked = entry.isSelectedForCycle,
            onCheckedChange = onSelectionChanged
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (entry.isPredefined) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = entry.hostname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
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

        Box {
            IconButton(
                onClick = { actionsExpanded = true }
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(
                        R.string.dns_entry_actions,
                        entry.name,
                        entry.hostname
                    )
                )
            }
            DropdownMenu(
                expanded = actionsExpanded,
                onDismissRequest = { actionsExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isActive) {
                                stringResource(R.string.dns_active)
                            } else {
                                stringResource(R.string.dns_set_active)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    },
                    enabled = !isActive && entry.hostname.isNotBlank(),
                    onClick = {
                        actionsExpanded = false
                        onSetActiveClicked()
                    }
                )

                if (entry.isPredefined && entry.descriptionResId != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.button_info_dns, entry.name)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                        },
                        onClick = {
                            actionsExpanded = false
                            onInfoClicked()
                        }
                    )
                } else if (!entry.isPredefined) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.button_edit)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        },
                        onClick = {
                            actionsExpanded = false
                            onEditClicked()
                        },
                        modifier = Modifier.testTag(
                            "dns_edit_button_${entry.hostname}_${entry.name}"
                        )
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.button_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            actionsExpanded = false
                            onDeleteClicked()
                        },
                        modifier = Modifier.testTag(
                            "dns_delete_button_${entry.hostname}_${entry.name}"
                        )
                    )
                }
            }
        }
    }
}
