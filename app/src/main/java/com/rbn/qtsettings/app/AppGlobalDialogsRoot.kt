package com.rbn.qtsettings.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ui.composables.permission.PermissionGrantDialog
import com.rbn.qtsettings.ui.composables.settings.AboutDialog
import com.rbn.qtsettings.viewmodel.MainViewModel

internal enum class AppOverlay { None, PermissionHelp, About }

/** Global overlay binding. Feature permission logic remains in its current owner (#121). */
@Composable
internal fun AppGlobalDialogsRoot(
    overlay: AppOverlay,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onShowPermissionHelp: () -> Unit,
    onOpenAdbSettings: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.toast_command_copied)
    when (overlay) {
        AppOverlay.None -> Unit
        AppOverlay.About -> AboutDialog(onDismiss, onShowPermissionHelp)
        AppOverlay.PermissionHelp -> {
            val shizukuAvailable by viewModel.isShizukuAvailable.collectAsStateWithLifecycle()
            val shizukuPermission by viewModel.appHasShizukuPermission.collectAsStateWithLifecycle()
            val rooted by viewModel.isDeviceRooted.collectAsStateWithLifecycle()
            PermissionGrantDialog(
                onDismissRequest = onDismiss,
                onOpenDeveloperOptions = onOpenAdbSettings,
                onCopyToClipboard = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", text))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
                onGrantWithShizuku = { viewModel.grantWriteSecureSettingsViaShizuku(context) },
                onGrantWithRoot = { viewModel.grantWriteSecureSettingsViaRoot(context) },
                onRequestShizukuPermission = onRequestShizukuPermission,
                isShizukuAvailable = shizukuAvailable,
                appHasShizukuPermission = shizukuPermission,
                isDeviceRooted = rooted
            )
        }
    }
}
