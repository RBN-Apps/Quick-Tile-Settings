package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppSearchField
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
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

/** Selection retains the existing immediate-apply behavior; Back without choosing is read-only. */
@Composable
internal fun AutomationDnsTargetScreen(mode: String, hostname: String?, providers: List<DnsHostnameEntry>,
                                       query: String, onQuery: (String) -> Unit,
                                       onSelect: (String, String?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().imePadding().testTag("automation_dns_target_page"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
        AppSearchField(query, onQuery, stringResource(R.string.wifi_rule_search_targets),
            Modifier.fillMaxWidth().testTag("automation_dns_target_search"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DnsTargetChoices(mode, hostname, providers, query, onSelect)
        }
    }
}
