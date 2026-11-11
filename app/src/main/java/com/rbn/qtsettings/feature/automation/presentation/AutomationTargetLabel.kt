package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON

@Composable
internal fun automationTargetLabel(mode: String, hostname: String?, providers: List<DnsHostnameEntry>): String =
    when (mode) {
        DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
        DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
        DNS_MODE_ON -> providers.firstOrNull { it.hostname == hostname }?.name ?: hostname.orEmpty()
        else -> stringResource(R.string.wifi_rule_action_default)
    }
