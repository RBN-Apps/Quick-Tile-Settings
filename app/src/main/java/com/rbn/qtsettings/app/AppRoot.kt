package com.rbn.qtsettings.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppNavigationContent
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.app.navigation.rememberAppNavigation
import com.rbn.qtsettings.feature.dns.presentation.DnsDialogsRoot
import com.rbn.qtsettings.feature.settings.presentation.NotificationDialogsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsRoot
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Composition root for the Activity-scoped legacy feature owner and the app shell. */
@Composable
fun AppRoot(
    viewModel: MainViewModel,
    usbViewModel: UsbSettingsViewModel,
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
    AppStatusEffects(viewModel, snackbar)
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(usbViewModel, lifecycle, resources) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            usbViewModel.events.collect { snackbar.showSnackbar(resources.getString(it)) }
        }
    }
    AppScaffold(
        area = navigation.area,
        onSelectArea = navigation::selectArea,
        destination = navigation.destination,
        snackbarHostState = snackbar,
        onBack = navigation::back,
        onOpenDestination = navigation::open,
        onShowPermissionHelp = showPermissionHelp,
        onShowAbout = { overlay = AppOverlay.About }
    ) { modifier ->
        AppNavigationContent(navigation, modifier) { route ->
            if (route == AppRoute.Usb || route == AppRoute.UsbTile ||
                route == AppRoute.UsbActions || route == AppRoute.UsbRevert) {
                UsbSettingsRoot(usbViewModel, showPermissionHelp, route, navigation::open)
            } else LegacyFeatureContentRoot(route, viewModel, showPermissionHelp)
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
    DnsDialogsRoot(viewModel)
    NotificationDialogsRoot(viewModel)
}
