package com.rbn.qtsettings.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R

@Composable
fun AppConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    icon: ImageVector? = null
) {
    AppDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        modifier = modifier,
        dismissLabel = stringResource(R.string.dialog_cancel),
        confirmEnabled = confirmEnabled,
        destructive = destructive,
        icon = icon
    ) {
        Text(message)
    }
}
