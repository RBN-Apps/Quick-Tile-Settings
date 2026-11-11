package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppInfoDialog
import com.rbn.qtsettings.data.DnsHostnameEntry

@Composable
fun DnsInfoDialog(entry: DnsHostnameEntry, onDismissRequest: () -> Unit) {
    AppInfoDialog(
        title = stringResource(R.string.dns_info_dialog_title, entry.name),
        message = entry.descriptionResId?.let { stringResource(it) }.orEmpty(),
        onDismissRequest = onDismissRequest
    )
}
