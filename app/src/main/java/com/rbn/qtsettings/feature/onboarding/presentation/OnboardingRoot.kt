package com.rbn.qtsettings.feature.onboarding.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.permissions.domain.GrantMethod
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import com.rbn.qtsettings.platform.permissions.adbPermissionGrantCommand
import kotlinx.coroutines.delay

/** Binds the first run to the existing permission owner; it never grants anything itself. */
@Composable
fun OnboardingRoot(
    viewModel: OnboardingViewModel,
    permissionViewModel: AppPermissionViewModel,
    onOpenDeveloperOptions: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onAddTile: (OnboardingTile) -> Unit,
    canAddTiles: Boolean,
    modifier: Modifier = Modifier
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val method by viewModel.method.collectAsStateWithLifecycle()
    val tilesInPanel by viewModel.tilesInPanel.collectAsStateWithLifecycle()
    val snapshot by permissionViewModel.state.collectAsStateWithLifecycle()
    val initialized by permissionViewModel.initialized.collectAsStateWithLifecycle()
    val operation by permissionViewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardLabel = stringResource(R.string.adb_instructions_title)
    val granted = snapshot.hasWriteSecureSettings
    // A device whose animations are switched off should not get a button that nudges forever.
    val motion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    LaunchedEffect(Unit) { permissionViewModel.refresh() }
    LaunchedEffect(granted) { if (granted) viewModel.onPermissionGranted() }
    // The permission usually arrives from a terminal on another machine, so the setup page looks
    // for it instead of waiting for the user to come back and press a button.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(step, granted, lifecycle) {
        if (step != OnboardingStep.Grant || granted) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(2_000)
                permissionViewModel.refreshQuietly()
            }
        }
    }

    val developerOptions by rememberUpdatedState(onOpenDeveloperOptions)
    val shizukuRequest by rememberUpdatedState(onRequestShizukuPermission)
    val tileRequest by rememberUpdatedState(onAddTile)
    val actions = remember(viewModel, permissionViewModel, context, clipboardLabel) {
        object : OnboardingUiActions {
            override fun show(step: OnboardingStep) = viewModel.show(step)
            override fun skip() = viewModel.finish()
            override fun finish() = viewModel.finish()
            override fun later() = viewModel.skipSetup()
            override fun chooseMethod(method: PermissionMethod) = viewModel.choose(method)
            override fun addTile(tile: OnboardingTile) = tileRequest(tile)
            override fun refresh() = permissionViewModel.refresh()
            override fun copyCommand(command: String) {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText(clipboardLabel, command))
                Toast.makeText(context, R.string.toast_command_copied, Toast.LENGTH_SHORT).show()
            }
            override fun openDeveloperOptions() = developerOptions()
            override fun requestShizukuPermission() = shizukuRequest()
            override fun grantWithShizuku() = permissionViewModel.grant(GrantMethod.Shizuku)
            override fun grantWithRoot() = permissionViewModel.grant(GrantMethod.Root)
        }
    }
    OnboardingScreen(
        OnboardingUiState(step, snapshot, initialized, operation, method,
            adbPermissionGrantCommand(context.packageName), operation.feedback?.resolve(resources),
            tilesInPanel, canAddTiles, motion),
        actions, modifier)
}
