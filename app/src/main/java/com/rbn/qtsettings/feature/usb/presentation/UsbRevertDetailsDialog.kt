package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog

/** The timer contract, kept off the page: what the timer restores, and what ends it. */
@Composable
fun UsbRevertDetailsDialog(onDismiss: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.usb_revert_details),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.dialog_close),
        onConfirm = onDismiss,
        icon = Icons.Outlined.Info
    ) {
        Text(stringResource(R.string.usb_revert_explanation))
        Text(stringResource(R.string.usb_revert_cancellation))
    }
}
