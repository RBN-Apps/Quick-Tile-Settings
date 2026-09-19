package com.rbn.qtsettings.app.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Current window constraints select presentation; content stays in one continuous composition. */
@Composable
fun AppNavigationScaffold(
    selectedTarget: AppRailTarget,
    onSelectTarget: (AppRailTarget) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (compact: Boolean, openNavigation: () -> Unit, onHeaderHeight: (Dp) -> Unit) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val layout = when {
            maxWidth < 600.dp -> AppRailLayout.Compact
            maxWidth < 840.dp -> AppRailLayout.Medium
            else -> AppRailLayout.Expanded
        }
        val compact = layout == AppRailLayout.Compact
        var expanded by key(layout) { rememberSaveable { mutableStateOf(layout == AppRailLayout.Expanded) } }
        val drawer = key(layout) { rememberDrawerState(DrawerValue.Closed) }
        var headerHeight by remember { mutableStateOf(64.dp) }
        val scope = rememberCoroutineScope()
        // The Material drawer supplies scrim, dismissal and predictive back. Its content geometry
        // is shared with the rail instead of using WideNavigationRail's fixed 44dp top/40dp gap.
        ModalNavigationDrawer(
            modifier = if (drawer.isOpen) Modifier.testTag("modal_navigation_open") else Modifier,
            drawerState = drawer,
            gesturesEnabled = compact,
            drawerContent = {
                if (compact) {
                    var sheetWidth by remember { mutableIntStateOf(0) }
                    val visible by remember(drawer) {
                        derivedStateOf {
                            drawer.currentValue == DrawerValue.Open || drawer.targetValue == DrawerValue.Open ||
                                (sheetWidth > 0 && drawer.currentOffset > -sheetWidth)
                        }
                    }
                    val sheetModifier = Modifier.railWidth(expanded = true)
                        .onSizeChanged { sheetWidth = it.width }
                        .testTag("modal_navigation_sheet")
                        .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
                    val railContent: @Composable () -> Unit = {
                        AppNavigationRail(true, true, headerHeight, selectedTarget,
                            onToggle = { scope.launch { drawer.close() } },
                            onSelectTarget = { target ->
                                onSelectTarget(target)
                                scope.launch { drawer.close() }
                            })
                    }
                    val openingOrOpen = drawer.currentValue == DrawerValue.Open || drawer.targetValue == DrawerValue.Open
                    if (openingOrOpen) {
                        // Register the modal back handler when opening, after the active Nav3
                        // entry's handler. It remains active throughout the closing animation.
                        ModalDrawerSheet(drawer, sheetModifier) {
                            railContent()
                        }
                    } else {
                        // Measure the same content while closed so the drag anchor never jumps
                        // when opening. Material leaves the closed sheet unplaced (no offscreen
                        // focus targets) and reveals it from the first drag delta. Only the back
                        // handler waits until opening, so it follows the active Nav3 entry.
                        ModalDrawerSheet(sheetModifier) { railContent() }
                    }
                }
            }
        ) {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (!compact) Surface(
                    modifier = Modifier.testTag("persistent_navigation_rail")
                        .animateContentSize(
                            animationSpec = railSpatialAnimation(),
                            alignment = Alignment.TopStart
                        ).railWidth(expanded),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AppNavigationRail(expanded, false, headerHeight, selectedTarget,
                        onToggle = { expanded = !expanded }, onSelectTarget = onSelectTarget)
                }
                content(compact, { scope.launch { drawer.open() } }, { headerHeight = it })
            }
        }
    }
}
