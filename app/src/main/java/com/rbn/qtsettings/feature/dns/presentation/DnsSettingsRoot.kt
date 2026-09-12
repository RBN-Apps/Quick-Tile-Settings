package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.app.PermissionWarningCard
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.data.DnsHostnameEntry

@Composable
fun DnsSettingsRoot(
    viewModel: DnsSettingsViewModel,
    onShowPermissionHelp: () -> Unit,
    route: AppRoute,
    onOpen: (AppDestination) -> Unit,
    automationContent: @Composable (List<DnsHostnameEntry>) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (!state.hasPermission) PermissionWarningCard(onShowPermissionHelp)
        when (route) {
            AppRoute.Dns -> DnsSettingsScreen(state, viewModel::onAction, onOpen)
            AppRoute.DnsProviders -> DnsProvidersScreen(state, viewModel::onAction, viewModel::reorder)
            AppRoute.DnsRevert -> DnsAutoRevertScreen(state, viewModel::onAction)
            AppRoute.DnsAutomation -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                automationContent(state.settings.providers.map { it.toRowEntry() })
            }
            else -> error("Not a DNS destination")
        }
    }
}
