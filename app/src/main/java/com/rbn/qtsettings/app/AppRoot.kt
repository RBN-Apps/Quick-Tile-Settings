package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppNavigationContent
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.app.navigation.rememberAppNavigation
import com.rbn.qtsettings.feature.automation.presentation.AutomationRoot
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.dns.presentation.DnsDialogsRoot
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsRoot
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.feature.settings.presentation.NotificationDialogsRoot
import com.rbn.qtsettings.feature.shortcuts.presentation.ShortcutSection
import com.rbn.qtsettings.feature.shortcuts.presentation.ShortcutSettingsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.feature.permissions.presentation.PermissionSettingsRoot
import com.rbn.qtsettings.feature.permissions.presentation.AdbInstructionsRoot
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsRoot
import com.rbn.qtsettings.feature.settings.presentation.AboutRoot

/** Binds feature owners to the app shell and Navigation 3 destinations. */
@Composable
fun AppRoot(
    viewModel: AppPermissionViewModel,
    settingsViewModel: AppSettingsViewModel,
    usbViewModel: UsbSettingsViewModel,
    dnsViewModel: DnsSettingsViewModel,
    automationViewModel: AutomationViewModel,
    shortcutViewModelFactory: ViewModelProvider.Factory,
    externalArea: AppArea?,
    onExternalAreaConsumed: () -> Unit,
    onOpenAdbSettings: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val navigation = rememberAppNavigation(externalArea ?: AppArea.Dns)
    val permissionState by viewModel.state.collectAsStateWithLifecycle()
    val initialized by viewModel.initialized.collectAsStateWithLifecycle()
    val hasPermission = permissionState.hasWriteSecureSettings
    val helpShown by viewModel.helpShown.collectAsStateWithLifecycle()
    val showPermissionHelp = {
        navigation.open(AppDestination.Permissions)
        viewModel.refresh()
    }
    LaunchedEffect(externalArea) {
        externalArea?.let {
            navigation.openTileArea(it)
            onExternalAreaConsumed()
        }
    }
    LaunchedEffect(initialized, hasPermission, helpShown, externalArea) {
        if (externalArea == null && initialized && !hasPermission && !helpShown) showPermissionHelp()
    }
    val snackbar = remember { SnackbarHostState() }
    AppStatusEffects(settingsViewModel, automationViewModel, snackbar)
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(usbViewModel, lifecycle, resources) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            usbViewModel.events.collect { snackbar.showSnackbar(resources.getString(it)) }
        }
    }
    LaunchedEffect(dnsViewModel, lifecycle, resources) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            dnsViewModel.events.collect { message ->
                snackbar.showSnackbar(if (message.argument == null) resources.getString(message.resource)
                    else resources.getString(message.resource, message.argument))
            }
        }
    }
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    AppScaffold(
        selectedTarget = navigation.selectedTarget,
        onSelectTarget = navigation::selectTarget,
        destination = navigation.destination,
        snackbarHostState = snackbar,
        onBack = {
            if (navigation.destination == com.rbn.qtsettings.app.navigation.AppDestination.WifiRule && backDispatcher != null)
                backDispatcher.onBackPressed()
            else navigation.back()
        }
    ) { modifier ->
        AppNavigationContent(navigation, modifier) { route ->
            if (route == AppRoute.Usb || route == AppRoute.UsbTile ||
                route == AppRoute.UsbActions || route == AppRoute.UsbRevert) {
                UsbSettingsRoot(usbViewModel, showPermissionHelp, route, { destination ->
                    if (destination == AppDestination.ShortcutSettings) navigation.openAreaShortcuts()
                    else navigation.open(destination)
                })
            } else if (route == AppRoute.Dns || route == AppRoute.DnsProviders ||
                route == AppRoute.DnsAutomation || route == AppRoute.DnsRevert ||
                route == AppRoute.DnsWifiTarget || route == AppRoute.DnsMobileTarget ||
                route == AppRoute.WifiRules || route is AppRoute.WifiRule) {
                DnsSettingsRoot(dnsViewModel, showPermissionHelp, route, { destination ->
                    if (destination == AppDestination.ShortcutSettings) navigation.openAreaShortcuts()
                    else navigation.open(destination)
                }) { automationRoute, providers ->
                    AutomationRoot(automationViewModel, providers, automationRoute,
                        onOpen = navigation::open, onEditRule = navigation::openWifiRule, onBack = navigation::back)
                }
            } else if (route is AppRoute.Shortcuts) {
                ShortcutSettingsRoot(shortcutViewModelFactory,
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    initialSection = if (!route.focusOrigin) null else when (route.origin) {
                        AppArea.Dns -> ShortcutSection.Dns
                        AppArea.Usb -> ShortcutSection.Usb
                    })
            } else if (route == AppRoute.Permissions) {
                PermissionSettingsRoot(
                    viewModel, onOpenAdb = { navigation.open(AppDestination.AdbInstructions) },
                    onOpenDeveloperOptions = onOpenAdbSettings,
                    onRequestShizukuPermission = onRequestShizukuPermission,
                    onOpenAutomation = navigation::openPermissionAutomation)
            } else if (route == AppRoute.AdbInstructions) {
                AdbInstructionsRoot()
            } else if (route == AppRoute.About) {
                AboutRoot(
                    onPermissionHelp = showPermissionHelp,
                    onShortcuts = { navigation.open(AppDestination.ShortcutSettings) },
                    onSettings = { navigation.open(AppDestination.AppSettings) }
                )
            } else if (route is AppRoute.Settings) {
                AppSettingsRoot(settingsViewModel, Modifier.padding(horizontal = 16.dp).fillMaxSize())
            }
        }
    }
    DnsDialogsRoot(dnsViewModel)
    NotificationDialogsRoot(automationViewModel, visible = navigation.destination != AppDestination.Permissions &&
        navigation.destination != AppDestination.AdbInstructions)
}
