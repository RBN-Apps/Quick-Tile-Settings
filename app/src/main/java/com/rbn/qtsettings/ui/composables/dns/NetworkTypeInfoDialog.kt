package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppInfoDialog

@Composable
fun NetworkTypeInfoDialog(onDismissRequest: () -> Unit) {
    AppInfoDialog(stringResource(R.string.network_type_info_title),
        stringResource(R.string.network_type_info_message), onDismissRequest)
}
