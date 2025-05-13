package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun DnsSettingsCard(viewModel: MainViewModel) {
    val dnsToggleOff by viewModel.dnsToggleOff.collectAsState()
    val dnsToggleAuto by viewModel.dnsToggleAuto.collectAsState()
    val dnsToggleOn by viewModel.dnsToggleOn.collectAsState()
    val dnsHostname by viewModel.dnsHostname.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setting_title_private_dns),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.setting_desc_tile_cycles),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CheckboxItem(
                checked = dnsToggleOff,
                onCheckedChange = { viewModel.setDnsToggleOff(it) },
                label = stringResource(R.string.dns_state_off)
            )
            CheckboxItem(
                checked = dnsToggleAuto,
                onCheckedChange = { viewModel.setDnsToggleAuto(it) },
                label = stringResource(R.string.dns_state_auto)
            )
            CheckboxItem(
                checked = dnsToggleOn,
                onCheckedChange = { viewModel.setDnsToggleOn(it) },
                label = stringResource(R.string.dns_state_on_with_host)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dnsHostname,
                onValueChange = { viewModel.setDnsHostname(it) },
                label = { Text(stringResource(R.string.dns_hostname_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = dnsToggleOn
            )
            if (dnsToggleOn && dnsHostname.isBlank()){
                Text(
                    text = stringResource(R.string.warning_hostname_blank),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CheckboxItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}