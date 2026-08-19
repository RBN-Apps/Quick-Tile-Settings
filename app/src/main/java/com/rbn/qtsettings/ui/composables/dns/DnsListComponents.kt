package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsListSortMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun DnsListHeader(
    sortMode: DnsListSortMode,
    onSortModeSelected: (DnsListSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.dns_select_hostnames_for_cycle),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag("dns_sort_menu_button")
            ) {
                Icon(
                    imageVector = if (sortMode == DnsListSortMode.MANUAL) {
                        Icons.Filled.DragHandle
                    } else {
                        Icons.Filled.SortByAlpha
                    },
                    contentDescription = stringResource(R.string.dns_sort_content_description)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dns_sort_manual)) },
                    leadingIcon = {
                        Icon(Icons.Filled.DragHandle, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortMode == DnsListSortMode.MANUAL) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortModeSelected(DnsListSortMode.MANUAL)
                    },
                    modifier = Modifier.testTag("dns_sort_manual")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dns_sort_alphabetical)) },
                    leadingIcon = {
                        Icon(Icons.Filled.SortByAlpha, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortMode == DnsListSortMode.ALPHABETICAL) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortModeSelected(DnsListSortMode.ALPHABETICAL)
                    },
                    modifier = Modifier.testTag("dns_sort_alphabetical")
                )
            }
        }
    }
}

@Composable
internal fun DnsModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isActive: Boolean,
    onSetActiveClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
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

        IconButton(
            onClick = onSetActiveClicked,
            enabled = !isActive
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.dns_set_active_mode, label)
            )
        }
    }
}

internal val DnsHostnameRowHeight = 64.dp

@Composable
internal fun DnsHostnameList(
    entries: List<DnsHostnameEntry>,
    sortMode: DnsListSortMode,
    activeDnsHostname: String?,
    scrollState: ScrollState,
    scrollViewport: Rect?,
    onSelectionChanged: (String, Boolean) -> Unit,
    onOrderCommitted: (List<String>) -> Boolean,
    onSetActive: (DnsHostnameEntry) -> Unit,
    onEdit: (DnsHostnameEntry) -> Unit,
    onDelete: (DnsHostnameEntry?) -> Unit,
    onInfo: (DnsHostnameEntry) -> Unit
) {
    val visualEntries = remember {
        androidx.compose.runtime.mutableStateListOf<DnsHostnameEntry>().apply {
            addAll(entries)
        }
    }
    var draggedEntryId by remember { mutableStateOf<String?>(null) }
    var liftedEntryId by remember { mutableStateOf<String?>(null) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    var draggedPointerY by remember { mutableFloatStateOf(0f) }
    var autoScrollDirection by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val itemHeightsPx = remember { mutableMapOf<String, Int>() }
    val animationScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val defaultRowHeightPx = with(density) {
        DnsHostnameRowHeight.toPx()
    }
    val autoScrollEdgePx = with(density) { 72.dp.toPx() }
    val autoScrollStepPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(entries, draggedEntryId) {
        if (draggedEntryId == null && visualEntries.toList() != entries) {
            visualEntries.clear()
            visualEntries.addAll(entries)
        }
    }

    fun restorePublishedEntries() {
        visualEntries.clear()
        visualEntries.addAll(entries)
    }

    fun commitVisualOrder(): Boolean {
        val committed = onOrderCommitted(visualEntries.map { it.id })
        if (!committed) {
            restorePublishedEntries()
        }
        return committed
    }

    fun moveEntry(entryId: String, targetIndex: Int): Boolean {
        val fromIndex = visualEntries.indexOfFirst { it.id == entryId }
        if (fromIndex == -1 || fromIndex == targetIndex || targetIndex !in visualEntries.indices) {
            return false
        }
        val movedEntry = visualEntries.removeAt(fromIndex)
        visualEntries.add(targetIndex, movedEntry)
        return true
    }

    fun applyDragDelta(entryId: String, delta: Float) {
        if (draggedEntryId != entryId) return
        draggedOffset += delta

        var currentIndex = visualEntries.indexOfFirst { it.id == entryId }
        while (currentIndex in 0 until visualEntries.lastIndex) {
            val nextEntry = visualEntries[currentIndex + 1]
            val distance = itemHeightsPx[nextEntry.id]?.toFloat() ?: defaultRowHeightPx
            if (draggedOffset <= distance / 2f) break
            moveEntry(entryId, currentIndex + 1)
            draggedOffset -= distance
            currentIndex += 1
        }
        while (currentIndex > 0) {
            val previousEntry = visualEntries[currentIndex - 1]
            val distance = itemHeightsPx[previousEntry.id]?.toFloat() ?: defaultRowHeightPx
            if (draggedOffset >= -distance / 2f) break
            moveEntry(entryId, currentIndex - 1)
            draggedOffset += distance
            currentIndex -= 1
        }

        val currentHeight = itemHeightsPx[entryId]?.toFloat() ?: defaultRowHeightPx
        draggedOffset = draggedOffset.coerceIn(-currentHeight, currentHeight)
    }

    fun updateAutoScrollDirection(dragDelta: Float) {
        val viewport = scrollViewport
        autoScrollDirection = when {
            draggedEntryId == null || viewport == null -> 0f
            dragDelta < 0f &&
                    draggedPointerY < viewport.top + autoScrollEdgePx &&
                    scrollState.canScrollBackward -> -1f

            dragDelta > 0f &&
                    draggedPointerY > viewport.bottom - autoScrollEdgePx &&
                    scrollState.canScrollForward -> 1f

            else -> 0f
        }
    }

    LaunchedEffect(draggedEntryId, autoScrollDirection, scrollState) {
        while (draggedEntryId != null && autoScrollDirection != 0f) {
            withFrameNanos { }
            val consumed = scrollState.scrollBy(autoScrollDirection * autoScrollStepPx)
            if (kotlin.math.abs(consumed) < 0.5f) {
                autoScrollDirection = 0f
                break
            }
            draggedEntryId?.let { entryId -> applyDragDelta(entryId, consumed) }
        }
    }

    LookaheadScope {
        val placementScope = this

        visualEntries.forEachIndexed { index, entry ->
            key(entry.id) {
                val ownsTranslation = draggedEntryId == entry.id
                val isLifted = liftedEntryId == entry.id
                val liftProgress by animateFloatAsState(
                    targetValue = if (isLifted) 1f else 0f,
                    animationSpec = tween(durationMillis = if (isLifted) 100 else 120),
                    label = "dnsDragLift"
                )
                val draggedContainerColor by animateColorAsState(
                    targetValue = if (isLifted) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    animationSpec = tween(durationMillis = if (isLifted) 100 else 120),
                    label = "dnsDragContainer"
                )
                val dragShape = MaterialTheme.shapes.small

                DnsHostnameRow(
                    entry = entry,
                    isActive = activeDnsHostname?.trim()
                        ?.equals(entry.hostname.trim(), ignoreCase = true) == true,
                    isDragged = isLifted,
                    reorderEnabled = sortMode == DnsListSortMode.MANUAL && visualEntries.size > 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < visualEntries.lastIndex,
                    onSelectionChanged = { selected -> onSelectionChanged(entry.id, selected) },
                    onSetActiveClicked = { onSetActive(entry) },
                    onEditClicked = { onEdit(entry) },
                    onDeleteClicked = { onDelete(entry) },
                    onInfoClicked = { onInfo(entry) },
                    onMoveUp = {
                        if (moveEntry(entry.id, index - 1)) commitVisualOrder()
                    },
                    onMoveDown = {
                        if (moveEntry(entry.id, index + 1)) commitVisualOrder()
                    },
                    onMeasured = { height -> itemHeightsPx[entry.id] = height },
                    onDragStart = { pointerY ->
                        settleJob?.cancel()
                        draggedEntryId = entry.id
                        liftedEntryId = entry.id
                        draggedOffset = 0f
                        draggedPointerY = pointerY
                    },
                    onDrag = { dragDelta ->
                        if (draggedEntryId == entry.id && dragDelta != 0f) {
                            draggedPointerY += dragDelta
                            applyDragDelta(entry.id, dragDelta)
                            updateAutoScrollDirection(dragDelta)
                        }
                    },
                    onDragStopped = {
                        autoScrollDirection = 0f
                        val settlingEntryId = draggedEntryId
                        val settlingOffset = draggedOffset
                        if (liftedEntryId == settlingEntryId) {
                            liftedEntryId = null
                        }

                        if (settlingEntryId == null || !commitVisualOrder()) {
                            settleJob?.cancel()
                            draggedEntryId = null
                            draggedOffset = 0f
                        } else {
                            settleJob?.cancel()
                            settleJob = animationScope.launch {
                                animate(
                                    initialValue = settlingOffset,
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 160,
                                        easing = LinearOutSlowInEasing
                                    )
                                ) { value, _ ->
                                    if (draggedEntryId == settlingEntryId) {
                                        draggedOffset = value
                                    }
                                }
                                if (draggedEntryId == settlingEntryId) {
                                    draggedOffset = 0f
                                    draggedEntryId = null
                                }
                            }
                        }
                    },
                    onDragCancelled = {
                        settleJob?.cancel()
                        autoScrollDirection = 0f
                        liftedEntryId = null
                        draggedEntryId = null
                        draggedOffset = 0f
                        restorePublishedEntries()
                    },
                    modifier = Modifier
                        .then(
                            if (ownsTranslation) {
                                Modifier
                            } else {
                                Modifier.animateBounds(lookaheadScope = placementScope)
                            }
                        )
                        .zIndex(if (ownsTranslation || liftProgress > 0.01f) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (ownsTranslation) draggedOffset else 0f
                            scaleX = 1f + (0.006f * liftProgress)
                            scaleY = 1f + (0.006f * liftProgress)
                        }
                        .then(
                            if (ownsTranslation || liftProgress > 0.001f) {
                                Modifier
                                    .dropShadow(dragShape) {
                                        radius = 6.dp.toPx()
                                        spread = 0f
                                        color = Color.Black
                                        alpha = 0.14f * liftProgress
                                        offset = Offset(0f, 2.dp.toPx() * liftProgress)
                                    }
                                    .clip(dragShape)
                                    .background(draggedContainerColor)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}
