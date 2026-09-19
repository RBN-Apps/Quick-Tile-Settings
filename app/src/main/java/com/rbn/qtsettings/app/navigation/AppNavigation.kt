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
    internal val stacks: Map<AppArea, NavBackStack<NavKey>>,
    internal val globalStack: NavBackStack<NavKey>
) {
    var area: AppArea by selectedArea
        private set
    private val activeStack get() = if (globalStack.isEmpty()) stacks.getValue(area) else globalStack
    val destination: AppDestination get() = (activeStack.last() as AppRoute).destination

    fun selectArea(selected: AppArea) {
        globalStack.clear()
        if (selected != area) area = selected
    }

    val selectedTarget: AppRailTarget get() = AppRailTarget.forDestination(destination, area)

    /** Rail navigation replaces the global path; content links use [open] to retain their caller. */
    fun selectTarget(target: AppRailTarget) {
        target.area?.let { selectArea(it); return }
        val route = globalRoute(checkNotNull(target.destination))
        if (globalStack.size == 1 && globalStack.last() == route) return
        globalStack.clear()
        globalStack.add(route)
    }

    private fun globalRoute(destination: AppDestination): AppRoute = when (destination) {
        AppDestination.ShortcutSettings -> AppRoute.Shortcuts(area)
        AppDestination.AppSettings -> AppRoute.Settings(area)
        AppDestination.About -> AppRoute.About
        AppDestination.Permissions -> AppRoute.Permissions
        AppDestination.AdbInstructions -> AppRoute.AdbInstructions
        else -> error("Not a global destination: $destination")
    }

    private val canOpenDns get() = area == AppArea.Dns || activeStack.contains(AppRoute.DnsAutomation)

    fun openPermissionAutomation() {
        if (activeStack.last() != AppRoute.DnsAutomation) activeStack.add(AppRoute.DnsAutomation)
    }

    fun open(destination: AppDestination) {
        val route = when (destination) {
            AppDestination.DnsProviders -> if (canOpenDns) AppRoute.DnsProviders else return
            AppDestination.DnsAutomation -> if (canOpenDns) AppRoute.DnsAutomation else return
            AppDestination.DnsWifiTarget -> if (canOpenDns) AppRoute.DnsWifiTarget else return
            AppDestination.DnsMobileTarget -> if (canOpenDns) AppRoute.DnsMobileTarget else return
            AppDestination.WifiRules -> if (canOpenDns) AppRoute.WifiRules else return
            AppDestination.WifiRule -> if (canOpenDns) AppRoute.WifiRule(null) else return
            AppDestination.DnsRevert -> if (canOpenDns) AppRoute.DnsRevert else return
            AppDestination.TileSettings -> return
            AppDestination.ShortcutSettings -> AppRoute.Shortcuts(area)
            AppDestination.ShortcutHelp -> AppRoute.ShortcutHelp
            AppDestination.AppSettings -> AppRoute.Settings(area)
            AppDestination.About -> AppRoute.About
            AppDestination.Permissions -> AppRoute.Permissions
            AppDestination.AdbInstructions -> AppRoute.AdbInstructions
            AppDestination.UsbTile -> if (area == AppArea.Usb) AppRoute.UsbTile else return
            AppDestination.UsbActions -> if (area == AppArea.Usb) AppRoute.UsbActions else return
            AppDestination.UsbRevert -> if (area == AppArea.Usb) AppRoute.UsbRevert else return
        }
        val targetStack = if (route is AppRoute.Shortcuts || route is AppRoute.Settings ||
            route == AppRoute.About || route == AppRoute.Permissions || route == AppRoute.AdbInstructions ||
            route == AppRoute.ShortcutHelp)
            globalStack else activeStack
        if (targetStack === globalStack) {
            val existing = targetStack.indexOf(route)
            if (existing >= 0) {
                while (targetStack.lastIndex > existing) targetStack.removeAt(targetStack.lastIndex)
                return
            }
        }
        if (targetStack.lastOrNull() != route) targetStack.add(route)
    }

    fun openAreaShortcuts() {
        val route = AppRoute.Shortcuts(area, focusOrigin = true)
        if (globalStack.lastOrNull() != route) globalStack.add(route)
    }

    /** Opens the editor for a stored rule; [open] with [AppDestination.WifiRule] starts a new one. */
    fun openWifiRule(ruleId: String) {
        if (!canOpenDns) return
        val route = AppRoute.WifiRule(ruleId)
        if (activeStack.last() != route) activeStack.add(route)
    }

    /** The target step stacks on the network step, so Back returns to the entered identity. */
    fun openWifiRuleTarget() {
        val current = activeStack.last() as? AppRoute.WifiRule ?: return
        if (!current.targetStep) activeStack.add(current.copy(targetStep = true))
    }

    /** Saving or deleting leaves the editor as a whole, whichever step the user confirmed from. */
    fun closeWifiRule() {
        while (activeStack.size > 1 && activeStack.last() is AppRoute.WifiRule) {
            activeStack.removeAt(activeStack.lastIndex)
        }
    }

    fun back() {
        if (globalStack.isNotEmpty()) globalStack.removeAt(globalStack.lastIndex)
        else if (activeStack.size > 1) activeStack.removeAt(activeStack.lastIndex)
    }

    /** External entry resets only its target stack; ordinary area selection preserves both. */
    fun openTileArea(target: AppArea) {
        globalStack.clear()
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
    val global = rememberNavBackStack(AppNavigationSavedState)
    return remember { AppNavigation(selectedArea, mapOf(AppArea.Dns to dns, AppArea.Usb to usb), global) }
}
