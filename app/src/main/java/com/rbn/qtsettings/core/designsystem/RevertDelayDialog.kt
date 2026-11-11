package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import com.rbn.qtsettings.R

/** Entry-owned draft survives recreation and only reaches settings after confirmation. */
@Composable
fun RevertDelayDialog(
    seconds: Int,
    onDismiss: () -> Unit,
    positiveOnly: Boolean = true,
    invalidValueMessage: String? = null,
    onSave: (Int) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(seconds.toString()) }
    val value = draft.toIntOrNull()?.takeIf { !positiveOnly || it > 0 }
    AppDialog(
        title = stringResource(R.string.setting_auto_revert_delay),
        onDismissRequest = onDismiss, confirmLabel = stringResource(R.string.usb_apply),
        onConfirm = { value?.let(onSave) }, dismissLabel = stringResource(android.R.string.cancel),
        confirmEnabled = value != null,
        // Let Android size this compact input window, as it does for the hostname editor.
        // Applying edge-to-edge insets after its first layout shifts the opening dialog.
        properties = DialogProperties(decorFitsSystemWindows = true)
    ) {
        AppTextField(
            value = draft, onValueChange = { draft = it },
            label = stringResource(R.string.usb_seconds_label),
            errorText = if (value == null) invalidValueMessage ?: stringResource(R.string.usb_delay_error) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
        )
    }
}
