package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rbn.qtsettings.data.DnsHostnameEntry

internal class AutomationDnsTargetViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val query = saved.getStateFlow("dnsTarget.query", "")
    fun search(value: String) { saved["dnsTarget.query"] = value }
}

@Composable
internal fun AutomationDnsTargetRoot(mode: String, hostname: String?, providers: List<DnsHostnameEntry>,
                                     onSelect: (String, String?) -> Unit) {
    val factory = remember { viewModelFactory { initializer { AutomationDnsTargetViewModel(createSavedStateHandle()) } } }
    val owner: AutomationDnsTargetViewModel = viewModel(factory = factory)
    val query by owner.query.collectAsStateWithLifecycle()
    AutomationDnsTargetScreen(mode, hostname, providers, query, owner::search, onSelect)
}
