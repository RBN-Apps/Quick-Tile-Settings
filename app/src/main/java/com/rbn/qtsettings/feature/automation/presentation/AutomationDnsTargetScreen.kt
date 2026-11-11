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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppSearchField
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.data.DnsHostnameEntry

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
