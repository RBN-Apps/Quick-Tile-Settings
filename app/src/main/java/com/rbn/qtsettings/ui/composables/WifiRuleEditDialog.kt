package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF

@Composable
internal fun WifiRuleEditDialog(
    seed: WifiRuleEditorSeed,
    dnsHostnames: List<DnsHostnameEntry>,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?, String, String?) -> Boolean
) {
    var ssid by rememberSaveable(seed) { mutableStateOf(seed.ssid.orEmpty()) }
    var bssid by rememberSaveable(seed) { mutableStateOf(seed.bssid.orEmpty()) }
    var action by rememberSaveable(seed) {
        mutableStateOf(seed.actionMode ?: DNS_MODE_OFF)
    }
    var hostname by rememberSaveable(seed) { mutableStateOf(seed.dnsHostname) }
    var error by rememberSaveable(seed) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(if (seed.ruleId == null) R.string.wifi_rule_add_title else R.string.wifi_rule_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it; error = false },
                    label = { Text(stringResource(R.string.wifi_ssid_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bssid,
                    onValueChange = { bssid = it; error = false },
                    label = { Text(stringResource(R.string.wifi_bssid_label)) },
                    supportingText = { Text(stringResource(R.string.wifi_bssid_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                seed.detectedBssid?.let { detectedBssid ->
                    TextButton(
                        onClick = { bssid = detectedBssid; error = false },
                        modifier = Modifier.testTag("wifi_rule_use_detected_bssid")
                    ) {
                        Text(stringResource(R.string.wifi_rule_use_detected_bssid, detectedBssid))
                    }
                }
                Text(
                    stringResource(R.string.wifi_rule_action_label),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                WifiRuleActionSelector(
                    action = action,
                    hostname = hostname,
                    dnsHostnames = dnsHostnames,
                    onActionSelected = { selectedAction, selectedHostname ->
                        action = selectedAction
                        hostname = selectedHostname
                        error = false
                    }
                )
                if (error) {
                    Text(
                        stringResource(R.string.wifi_rule_invalid_or_duplicate),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = !onSave(
                    seed.ruleId,
                    ssid.takeIf { it.isNotBlank() },
                    bssid.takeIf { it.isNotBlank() },
                    action,
                    hostname
                )
            }) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}
