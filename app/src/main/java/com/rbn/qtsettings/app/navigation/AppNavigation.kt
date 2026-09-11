package com.rbn.qtsettings.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/** UI navigation state only. Features receive callbacks, never the mutable back stacks. */
class AppNavigation internal constructor(
    selectedArea: MutableState<AppArea>,
    internal val stacks: Map<AppArea, NavBackStack<NavKey>>
) {
    var area: AppArea by selectedArea
        private set
    private val activeStack get() = stacks.getValue(area)
    val destination: AppDestination get() = (activeStack.last() as AppRoute).destination

    fun selectArea(selected: AppArea) {
        if (selected != area) area = selected
    }

    fun open(destination: AppDestination) {
        val route = when (destination) {
            AppDestination.TileSettings -> return
            AppDestination.ShortcutSettings -> AppRoute.Shortcuts(area)
            AppDestination.AppSettings -> AppRoute.Settings(area)
            AppDestination.UsbTile -> if (area == AppArea.Usb) AppRoute.UsbTile else return
            AppDestination.UsbActions -> if (area == AppArea.Usb) AppRoute.UsbActions else return
            AppDestination.UsbRevert -> if (area == AppArea.Usb) AppRoute.UsbRevert else return
        }
        if (activeStack.last() != route) activeStack.add(route)
    }

    fun back() {
        if (activeStack.size > 1) activeStack.removeAt(activeStack.lastIndex)
    }

    /** External entry resets only its target stack; ordinary area selection preserves both. */
    fun openTileArea(target: AppArea) {
        val stack = stacks.getValue(target)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        area = target
    }
}

@Composable
fun rememberAppNavigation(initialArea: AppArea = AppArea.Dns): AppNavigation {
    val selectedArea = rememberSaveable { mutableStateOf(initialArea) }
    val dns = rememberNavBackStack(AppNavigationSavedState, AppRoute.Dns)
    val usb = rememberNavBackStack(AppNavigationSavedState, AppRoute.Usb)
    return remember { AppNavigation(selectedArea, mapOf(AppArea.Dns to dns, AppArea.Usb to usb)) }
}
