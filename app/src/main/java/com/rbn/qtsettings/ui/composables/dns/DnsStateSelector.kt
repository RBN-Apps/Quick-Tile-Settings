package com.rbn.qtsettings.ui.composables.dns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON

@Composable
fun DnsStateSelector(
    dnsState: String,
    dnsHostname: String?,
    dnsHostnames: List<DnsHostnameEntry>,
    onDnsStateChange: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = when (dnsState) {
                    DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
                    DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
                    DNS_MODE_ON -> {
                        val entry = dnsHostnames.find { it.hostname == dnsHostname }
                        entry?.name ?: dnsHostname
                        ?: stringResource(R.string.setting_select_hostname)
                    }

                    else -> stringResource(R.string.dns_mode_auto_label)
                },
                modifier = Modifier.weight(1f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dns_mode_off_label)) },
                onClick = {
                    onDnsStateChange(DNS_MODE_OFF, null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dns_mode_auto_label)) },
                onClick = {
                    onDnsStateChange(DNS_MODE_AUTO, null)
                    expanded = false
                }
            )
            HorizontalDivider()
            dnsHostnames.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(entry.name)
                            Text(
                                text = entry.hostname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onDnsStateChange(DNS_MODE_ON, entry.hostname)
                        expanded = false
                    }
                )
            }
        }
    }
}
