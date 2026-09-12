package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiNetworkIdentity

@Composable
internal fun WifiNetworkPickerDialog(
    currentNetwork: WifiNetworkIdentity?,
    scannedNetworks: List<WifiNetworkIdentity>,
    knownNetworks: List<KnownWifiNetwork>,
    isLoadingNetworks: Boolean,
    onRefresh: () -> Unit,
    onSelect: (WifiNetworkIdentity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.wifi_network_picker_title)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                currentNetwork?.let {
                    CandidateSectionTitle(R.string.wifi_network_current_title)
                    CandidateRow(it, onSelect)
                }
                CandidateSectionTitle(R.string.wifi_network_nearby_title)
                if (isLoadingNetworks) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wifi_network_scanning))
                    }
                } else if (scannedNetworks.isEmpty()) {
                    Text(stringResource(R.string.wifi_network_none_found), style = MaterialTheme.typography.bodySmall)
                } else {
                    scannedNetworks.forEach { CandidateRow(it, onSelect) }
                }
                if (knownNetworks.isNotEmpty()) {
                    CandidateSectionTitle(R.string.wifi_network_history_title)
                    knownNetworks.forEach { CandidateRow(it.identity, onSelect) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !isLoadingNetworks) { Text(stringResource(R.string.wifi_network_refresh)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun CandidateSectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}

@Composable
private fun CandidateRow(identity: WifiNetworkIdentity, onSelect: (WifiNetworkIdentity) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onSelect(identity) }.padding(vertical = 8.dp)) {
        Text(identity.ssid ?: stringResource(R.string.wifi_hidden_network))
        identity.bssid?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
