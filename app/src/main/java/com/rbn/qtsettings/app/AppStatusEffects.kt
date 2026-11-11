package com.rbn.qtsettings.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch

/** Bind pending legacy messages only while the UI is started; acknowledge before display. */
@Composable
fun AppStatusEffects(viewModel: AppSettingsViewModel, automation: AutomationViewModel, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, automation, snackbarHostState, lifecycle, resources) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.events.collect { message ->
                    snackbarHostState.showSnackbar(message.resolve(resources))
                }
            }
            launch {
                automation.networkDetectionStatusMessage.collect { message ->
                    if (message != null) {
                        automation.clearNetworkDetectionStatusMessage()
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }
}
