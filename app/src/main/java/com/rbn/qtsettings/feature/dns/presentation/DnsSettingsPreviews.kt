package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.dns.domain.*
import com.rbn.qtsettings.ui.composables.dns.DnsSettingsCard

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DnsProviderSettingsPreview() {
    QuickTileSettingsTheme {
        DnsSettingsCard(
            state = DnsSettingsState(
                settings = DnsSettings(providers = listOf(
                    DnsProvider("home", "Home DNS", DnsTarget.Hostname("dns.example.com"), false, true)
                )),
                active = DnsTarget.Hostname("dns.example.com"), hasPermission = true
            ),
            onAction = {}, onReorder = { true }, automationContent = {}
        )
    }
}
