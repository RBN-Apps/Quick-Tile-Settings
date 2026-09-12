package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.dns.domain.DnsTarget

@Composable
internal fun DnsStatusCard(state: DnsSettingsState) {
    val active = state.active
    val provider = (active as? DnsTarget.Hostname)?.let { target ->
        state.settings.providers.find { it.target.value.equals(target.value, ignoreCase = true) }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.setting_title_private_dns), style = MaterialTheme.typography.titleMedium)
            Text(when (active) {
                DnsTarget.Off -> stringResource(R.string.dns_state_off)
                DnsTarget.Automatic -> stringResource(R.string.dns_state_auto)
                is DnsTarget.Hostname -> provider?.name ?: active.value
                null -> stringResource(R.string.dns_status_unavailable)
            }, style = MaterialTheme.typography.headlineSmall)
            if (active is DnsTarget.Hostname && provider != null) Text(active.value, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.dns_status_summary), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
