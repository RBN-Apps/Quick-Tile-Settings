package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsChoiceRow
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

@Composable
internal fun DnsTargetChoices(mode: String, hostname: String?, providers: List<DnsHostnameEntry>, query: String,
                              onTarget: (String, String?) -> Unit, wifiDefaultLabel: String? = null) {
    val keys = (if (wifiDefaultLabel != null) listOf("default", "off", "auto") else listOf("off", "auto")) + providers.map { it.id }
    val matchingKeys = keys.filter { key ->
        val title = when (key) {
            "default" -> stringResource(R.string.wifi_rule_action_default) + " " + wifiDefaultLabel
            "off" -> stringResource(R.string.dns_mode_off_label)
            "auto" -> stringResource(R.string.dns_mode_auto_label)
            else -> providers.first { it.id == key }.let { it.name + " " + it.hostname }
        }
        title.contains(query.trim(), ignoreCase = true)
    }
    if (matchingKeys.isEmpty()) SupportingText(stringResource(R.string.wifi_rule_search_empty))
    SettingsGroup(matchingKeys, { it }, Modifier.selectableGroup()) { key ->
        when (key) {
            "default" -> SettingsChoiceRow(stringResource(R.string.wifi_rule_action_default),
                mode == WIFI_DNS_ACTION_DEFAULT, { onTarget(WIFI_DNS_ACTION_DEFAULT, null) },
                Modifier.testTag("automation_target_default"),
                summary = stringResource(R.string.wifi_rule_action_default_summary, wifiDefaultLabel.orEmpty()),
                icon = Icons.Outlined.Wifi)
            "off" -> SettingsChoiceRow(stringResource(R.string.dns_mode_off_label), mode == DNS_MODE_OFF,
                { onTarget(DNS_MODE_OFF, null) }, Modifier.testTag("automation_target_off"),
                icon = ImageVector.vectorResource(R.drawable.ic_dns_off))
            "auto" -> SettingsChoiceRow(stringResource(R.string.dns_mode_auto_label), mode == DNS_MODE_AUTO,
                { onTarget(DNS_MODE_AUTO, null) }, Modifier.testTag("automation_target_auto"),
                icon = ImageVector.vectorResource(R.drawable.ic_dns_auto))
            else -> providers.first { it.id == key }.let { provider ->
                SettingsChoiceRow(provider.name, mode == DNS_MODE_ON && hostname == provider.hostname,
                    { onTarget(DNS_MODE_ON, provider.hostname) }, Modifier.testTag("automation_target_${provider.id}"),
                    summary = provider.hostname,
                    icon = ImageVector.vectorResource(when (provider.hostname) {
                        "dns.adguard.com" -> R.drawable.ic_dns_on_adguard
                        "one.one.one.one" -> R.drawable.ic_dns_on_cloudflare
                        "dns.quad9.net" -> R.drawable.ic_dns_on_quad9_security
                        else -> R.drawable.ic_dns_on
                    }))
            }
        }
    }
}
