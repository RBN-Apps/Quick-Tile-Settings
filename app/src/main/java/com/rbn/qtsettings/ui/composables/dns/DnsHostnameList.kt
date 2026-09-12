package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsListSortMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
