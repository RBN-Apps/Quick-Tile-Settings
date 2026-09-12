package com.rbn.qtsettings.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/** Bind pending legacy messages only while the UI is started; acknowledge before display. */
@Composable
fun AppStatusEffects(viewModel: MainViewModel, automation: AutomationViewModel, snackbarHostState: SnackbarHostState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, automation, snackbarHostState, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.backupStatusMessage.collect { message ->
                    if (message != null) {
                        viewModel.clearBackupStatusMessage()
                        snackbarHostState.showSnackbar(message)
                    }
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
