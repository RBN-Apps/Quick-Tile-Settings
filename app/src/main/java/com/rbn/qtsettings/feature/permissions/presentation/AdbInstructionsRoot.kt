package com.rbn.qtsettings.feature.permissions.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.platform.permissions.adbPermissionGrantCommand

@Composable
fun AdbInstructionsRoot() {
    val context = LocalContext.current
    val label = stringResource(R.string.adb_instructions_title)
    AdbInstructionsScreen(adbPermissionGrantCommand(context.packageName)) { command ->
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, command))
        Toast.makeText(context, R.string.toast_command_copied, Toast.LENGTH_SHORT).show()
    }
}
