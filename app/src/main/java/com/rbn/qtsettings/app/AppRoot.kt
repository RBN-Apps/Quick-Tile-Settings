package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.rbn.qtsettings.feature.shortcuts.presentation.ShortcutSettingsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Composition root for the Activity-scoped legacy feature owner and the app shell. */
@Composable
fun AppRoot(
    viewModel: MainViewModel,
    usbViewModel: UsbSettingsViewModel,
    dnsViewModel: DnsSettingsViewModel,
    automationViewModel: AutomationViewModel,
    shortcutViewModelFactory: ViewModelProvider.Factory,
    externalArea: AppArea?,
    onExternalAreaConsumed: () -> Unit,
    onOpenAdbSettings: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    var overlay by rememberSaveable { mutableStateOf(AppOverlay.None) }
    val hasPermission by viewModel.hasWriteSecureSettings.collectAsStateWithLifecycle()
    val helpShown by viewModel.helpShown.collectAsStateWithLifecycle()
    val showPermissionHelp = {
        overlay = AppOverlay.PermissionHelp
        viewModel.checkSystemStates(context)
    }
    LaunchedEffect(hasPermission, helpShown) {
        if (!hasPermission && !helpShown) showPermissionHelp()
    }
    val navigation = rememberAppNavigation(externalArea ?: AppArea.Dns)
    LaunchedEffect(externalArea) {
        externalArea?.let {
            navigation.openTileArea(it)
            onExternalAreaConsumed()
        }
    }
    val snackbar = remember { SnackbarHostState() }
    AppStatusEffects(viewModel, automationViewModel, snackbar)
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
        area = navigation.area,
        onSelectArea = navigation::selectArea,
        destination = navigation.destination,
        snackbarHostState = snackbar,
        onBack = {
            if (navigation.destination == com.rbn.qtsettings.app.navigation.AppDestination.WifiRule && backDispatcher != null)
                backDispatcher.onBackPressed()
            else navigation.back()
        },
        onOpenDestination = navigation::open,
        onShowPermissionHelp = showPermissionHelp,
        onShowAbout = { overlay = AppOverlay.About }
    ) { modifier ->
        AppNavigationContent(navigation, modifier) { route ->
            if (route == AppRoute.Usb || route == AppRoute.UsbTile ||
                route == AppRoute.UsbActions || route == AppRoute.UsbRevert) {
                UsbSettingsRoot(usbViewModel, showPermissionHelp, route, navigation::open)
            } else if (route == AppRoute.Dns || route == AppRoute.DnsProviders ||
                route == AppRoute.DnsAutomation || route == AppRoute.DnsRevert ||
                route == AppRoute.DnsWifiTarget || route == AppRoute.DnsMobileTarget ||
                route == AppRoute.WifiRules || route is AppRoute.WifiRule) {
                DnsSettingsRoot(dnsViewModel, showPermissionHelp, route, navigation::open) { automationRoute, providers ->
                    AutomationRoot(automationViewModel, providers, automationRoute,
                        onOpen = navigation::open, onEditRule = navigation::openWifiRule, onBack = navigation::back)
                }
            } else if (route is AppRoute.Shortcuts) {
                ShortcutSettingsRoot(shortcutViewModelFactory,
                    Modifier.fillMaxSize().padding(horizontal = 16.dp))
            } else LegacyFeatureContentRoot(route, viewModel)
        }
    }
    AppGlobalDialogsRoot(
        overlay = overlay,
        viewModel = viewModel,
        onDismiss = {
            if (overlay == AppOverlay.PermissionHelp && !hasPermission) viewModel.setHelpShown(true)
            overlay = AppOverlay.None
        },
        onShowPermissionHelp = showPermissionHelp,
        onOpenAdbSettings = onOpenAdbSettings,
        onRequestShizukuPermission = onRequestShizukuPermission
    )
    DnsDialogsRoot(dnsViewModel)
    NotificationDialogsRoot(automationViewModel)
}
