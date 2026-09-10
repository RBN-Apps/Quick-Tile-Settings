package com.rbn.qtsettings.app.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/** Presentation variants only; Navigation 3 owns area selection and back stacks. */
enum class AppRailLayout { Compact, Medium, Expanded }

/** Shared, non-overshooting motion for the rail width and item spacing. */
internal fun <T> railSpatialAnimation(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 700f)

/** Shared geometry for the persistent rail and the modal sheet. No container-imposed top gaps. */
@Composable
fun AppNavigationRail(
    expanded: Boolean,
    modal: Boolean,
    headerHeight: Dp,
    area: AppArea,
    onToggle: () -> Unit,
    onSelectArea: (AppArea) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemSpacing by animateDpAsState(
        if (expanded) 0.dp else AppSpacing.contentGap,
        animationSpec = railSpatialAnimation(),
        label = "Rail item spacing"
    )
    Column(modifier.fillMaxHeight()) {
        Box(
            Modifier.height(headerHeight).fillMaxWidth()
                .padding(start = if (modal) 4.dp else AppSpacing.iconSize),
            contentAlignment = Alignment.CenterStart
        ) { RailToggle(expanded, onToggle) }
        Column(
            Modifier.selectableGroup().verticalScroll(rememberScrollState()).padding(top = AppSpacing.contentGap),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            AppArea.entries.forEach { target ->
                AppNavigationRailItem(target, target == area, expanded) { onSelectArea(target) }
            }
        }
    }
}

/** Intrinsic text width up to a readable rail width; collapsed mode has a stable narrow column. */
internal fun Modifier.railWidth(expanded: Boolean): Modifier =
    widthIn(min = if (expanded) 220.dp else 96.dp, max = if (expanded) 360.dp else 96.dp)
        .width(IntrinsicSize.Max)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RailToggle(expanded: Boolean, onToggle: () -> Unit) {
    val description = stringResource(if (expanded) R.string.navigation_collapse else R.string.navigation_expand)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = description }) {
            Crossfade(expanded, label = "Rail menu icon") { open ->
                Icon(if (open) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu, null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavigationRailItem(area: AppArea, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    // Use the actual Material placement instead of duplicating its private animation timing.
    // During expansion the full name must not flash in the outgoing below-icon position.
    var iconBottom by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    var labelTop by remember { mutableFloatStateOf(0f) }
    val label = stringResource(if (area == AppArea.Dns) R.string.tab_title_dns else R.string.tab_title_usb)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        WideNavigationRailItem(
            selected = selected,
            onClick = onClick,
            railExpanded = expanded,
            modifier = Modifier.heightIn(min = AppSpacing.minimumTouchTarget).testTag("navigation_area_${area.name}")
                .semantics { if (!expanded) contentDescription = label },
            icon = {
                Icon(
                    if (area == AppArea.Dns) Icons.Default.Dns else Icons.Default.Usb,
                    contentDescription = null,
                    modifier = Modifier.testTag("navigation_icon_${area.name}")
                        .onGloballyPositioned { iconBottom = it.boundsInRoot().bottom }
                )
            },
            label = {
                // WideNavigationRailItem already fades and moves its label. A nested Crossfade
                // retains the outgoing full label's width, misaligning the short label and then
                // snapping the item height when that outgoing composition disappears.
                if (expanded) Text(label, modifier = Modifier
                    .onGloballyPositioned { labelTop = it.boundsInRoot().top }
                    .graphicsLayer { alpha = if (labelTop >= iconBottom) 0f else 1f }
                )
                else Text(
                    stringResource(if (area == AppArea.Dns) R.string.navigation_dns_short else R.string.navigation_usb_short),
                    // The parent exposes the full destination name once to accessibility.
                    modifier = Modifier.testTag("navigation_short_label_${area.name}").clearAndSetSemantics { }
                )
            }
        )
    }
}
