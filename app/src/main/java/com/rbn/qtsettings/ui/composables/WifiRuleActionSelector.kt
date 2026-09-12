package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

@Composable
internal fun WifiRuleActionSelector(
    action: String,
    hostname: String?,
    dnsHostnames: List<DnsHostnameEntry>,
    onActionSelected: (String, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(
            when (action) {
                WIFI_DNS_ACTION_DEFAULT -> stringResource(R.string.wifi_rule_action_default)
                DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
                DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
                DNS_MODE_ON -> dnsHostnames.find { it.hostname == hostname }?.name ?: hostname.orEmpty()
                else -> stringResource(R.string.wifi_rule_action_default)
            },
            modifier = Modifier.weight(1f)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.wifi_rule_action_default)) },
            onClick = { onActionSelected(WIFI_DNS_ACTION_DEFAULT, null); expanded = false }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dns_mode_off_label)) },
            onClick = { onActionSelected(DNS_MODE_OFF, null); expanded = false }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dns_mode_auto_label)) },
            onClick = { onActionSelected(DNS_MODE_AUTO, null); expanded = false }
        )
        if (dnsHostnames.isNotEmpty()) HorizontalDivider()
        dnsHostnames.forEach { entry ->
            DropdownMenuItem(
                text = { Column { Text(entry.name); Text(entry.hostname, style = MaterialTheme.typography.bodySmall) } },
                onClick = { onActionSelected(DNS_MODE_ON, entry.hostname); expanded = false }
            )
        }
    }
}

internal data class WifiRuleEditorSeed(
    val ruleId: String? = null,
    val ssid: String? = null,
    val bssid: String? = null,
    val actionMode: String? = null,
    val dnsHostname: String? = null,
    val detectedBssid: String? = null
) {
    companion object {
        fun fromRule(rule: WifiDnsRule) = WifiRuleEditorSeed(
            ruleId = rule.id,
            ssid = rule.ssid,
            bssid = rule.bssid,
            actionMode = rule.actionMode,
            dnsHostname = rule.dnsHostname
        )

        fun fromIdentity(identity: WifiNetworkIdentity) = WifiRuleEditorSeed(
            ssid = identity.ssid,
            detectedBssid = identity.bssid
        )

        val NullableSaver = listSaver<WifiRuleEditorSeed?, String>(
            save = { seed ->
                if (seed == null) {
                    emptyList()
                } else {
                    listOf(
                        seed.ruleId.orEmpty(),
                        seed.ssid.orEmpty(),
                        seed.bssid.orEmpty(),
                        seed.actionMode.orEmpty(),
                        seed.dnsHostname.orEmpty(),
                        seed.detectedBssid.orEmpty()
                    )
                }
            },
            restore = { values ->
                if (values.isEmpty()) {
                    null
                } else {
                    WifiRuleEditorSeed(
                        ruleId = values[0].ifEmpty { null },
                        ssid = values[1].ifEmpty { null },
                        bssid = values[2].ifEmpty { null },
                        actionMode = values[3].ifEmpty { null },
                        dnsHostname = values[4].ifEmpty { null },
                        detectedBssid = values.getOrNull(5)?.ifEmpty { null }
                    )
                }
            }
        )
    }
}
