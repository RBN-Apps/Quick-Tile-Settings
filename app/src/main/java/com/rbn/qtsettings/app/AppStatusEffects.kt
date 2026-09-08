package com.rbn.qtsettings.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/** Bind pending legacy messages only while the UI is started; acknowledge before display. */
@Composable
fun AppStatusEffects(viewModel: MainViewModel, snackbarHostState: SnackbarHostState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, snackbarHostState, lifecycle) {
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
                viewModel.quickActionStatusMessage.collect { message ->
                    if (message != null) {
                        viewModel.clearQuickActionStatusMessage()
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
            launch {
                viewModel.networkDetectionStatusMessage.collect { message ->
                    if (message != null) {
                        viewModel.clearNetworkDetectionStatusMessage()
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }
}
