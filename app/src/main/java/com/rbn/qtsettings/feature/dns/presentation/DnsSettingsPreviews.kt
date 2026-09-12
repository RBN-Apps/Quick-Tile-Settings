package com.rbn.qtsettings.feature.dns.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.dns.domain.*

private val previewState = DnsSettingsState(
    settings = DnsSettings(providers = listOf(
        DnsProvider("home", "Home DNS", DnsTarget.Hostname("dns.example.com"), false, true),
        DnsProvider("work", "Work network", DnsTarget.Hostname("dns.work.example.com"), false, false)
    )), active = DnsTarget.Hostname("dns.example.com"), hasPermission = true
)

@Preview(showBackground = true, heightDp = 780)
@Preview(showBackground = true, heightDp = 900, fontScale = 1.5f, locale = "de")
@Composable
private fun DnsOverviewPreview() = QuickTileSettingsTheme { DnsSettingsScreen(previewState, {}, {}) }

@Preview(showBackground = true, heightDp = 780)
@Preview(showBackground = true, heightDp = 780, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DnsProvidersPreview() = QuickTileSettingsTheme { DnsProvidersScreen(previewState, {}, { true }) }

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DnsAutoRevertPreview() = QuickTileSettingsTheme {
    DnsAutoRevertScreen(previewState.copy(settings = previewState.settings.copy(enableAutoRevert = true)), {})
}
