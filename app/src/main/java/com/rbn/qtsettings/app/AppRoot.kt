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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.rememberLegacyAppNavigation
import com.rbn.qtsettings.feature.dns.presentation.DnsDialogsRoot
import com.rbn.qtsettings.feature.settings.presentation.NotificationDialogsRoot
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Composition root for the Activity-scoped legacy feature owner and the app shell. */
@Composable
fun AppRoot(
    viewModel: MainViewModel,
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
    val navigation = rememberLegacyAppNavigation(externalArea, onExternalAreaConsumed, overlay == AppOverlay.None)
    val snackbar = remember { SnackbarHostState() }
    AppStatusEffects(viewModel, snackbar)
    AppScaffold(
        destination = navigation.destination,
        snackbarHostState = snackbar,
        onBack = navigation::back,
        onOpenDestination = navigation::open,
        onShowPermissionHelp = showPermissionHelp,
        onShowAbout = { overlay = AppOverlay.About }
    ) { destination ->
        LegacyFeatureContentRoot(destination, viewModel, navigation.pagerState, showPermissionHelp)
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
