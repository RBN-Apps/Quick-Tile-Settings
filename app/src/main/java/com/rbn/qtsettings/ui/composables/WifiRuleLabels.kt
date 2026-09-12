package com.rbn.qtsettings.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

internal fun WifiDnsRule.displayIdentity(): String = listOfNotNull(ssid, bssid).joinToString(" / ")

@Composable
internal fun WifiDnsRule.actionLabel(hostnames: List<DnsHostnameEntry>): String = when (actionMode) {
    WIFI_DNS_ACTION_DEFAULT -> stringResource(R.string.wifi_rule_action_default)
    DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
    DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
    DNS_MODE_ON -> hostnames.find { it.hostname == dnsHostname }?.name ?: dnsHostname.orEmpty()
    else -> stringResource(R.string.wifi_rule_action_default)
}
