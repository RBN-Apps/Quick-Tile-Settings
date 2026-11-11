package com.rbn.qtsettings.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R

@Composable
fun AppInfoDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    AppDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(R.string.dialog_close),
        onConfirm = onDismissRequest,
        modifier = modifier,
        icon = icon
    ) {
        Text(message)
    }
}
