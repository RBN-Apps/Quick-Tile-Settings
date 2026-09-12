package com.rbn.qtsettings.feature.automation.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.*
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON

private val providers = listOf(DnsHostnameEntry("preview", "Example DNS", "dns.example.com"))
private val previewState = AutomationUiState(
    vpnDetectionEnabled = true, vpnDetectionMode = BACKGROUND_DETECTION,
    networkTypeDetectionEnabled = true, networkTypeDetectionMode = BACKGROUND_DETECTION,
    dnsStateOnWifi = DNS_MODE_ON, dnsHostnameOnWifi = "dns.example.com",
    wifiNetworkRulesEnabled = true,
    wifiNetworkRules = listOf(
        WifiDnsRule("preview", "Office network with a long name", actionMode = DNS_MODE_AUTO),
        WifiDnsRule("home", "Home", "AA:BB:CC:DD:EE:FF", DNS_MODE_ON, "dns.example.com")
    ),
    knownWifiNetworks = listOf(KnownWifiNetwork("Café")),
    currentWifiNetwork = WifiNetworkIdentity("Office network with a long name", "AA:BB:CC:DD:EE:FF"),
    scannedWifiNetworks = listOf(WifiNetworkIdentity("Guest", "11:22:33:44:55:66"), WifiNetworkIdentity(null, "77:88:99:AA:BB:CC")),
    isWifiResultsLoading = false, wifiIdentityAccessState = WifiIdentityAccessState.AVAILABLE,
    hasBackgroundLocationPermission = true, dnsStateOnMobile = DNS_MODE_AUTO, dnsHostnameOnMobile = null
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, heightDp = 900, locale = "de", fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 640, heightDp = 360, locale = "de")
@Composable
private fun AutomationPreview() = QuickTileSettingsTheme {
    Surface { Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        AutomationScreen(previewState, PreviewActions, providers, {}, {}, {})
    } }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Preview(showBackground = true, widthDp = 360, heightDp = 900, locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RulesPreview() = QuickTileSettingsTheme {
    Surface { WifiRulesScreen(previewState, providers, {}, {}, {}, {}, {}, {}, Modifier.padding(16.dp)) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun EmptyRulesWithoutPermissionPreview() = QuickTileSettingsTheme {
    Surface { WifiRulesScreen(previewState.copy(wifiNetworkRules = emptyList(),
        wifiIdentityAccessState = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED),
        providers, {}, {}, {}, {}, {}, {}, Modifier.padding(16.dp)) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1100)
@Preview(showBackground = true, widthDp = 640, heightDp = 360, locale = "de", fontScale = 1.5f)
@Composable
private fun RuleEditorPreview() = QuickTileSettingsTheme {
    Surface(Modifier.fillMaxSize()) {
        WifiRuleEditorScreen(
            WifiRuleDraft("home", "Office network with a long name", "AA:BB:CC:DD:EE:FF", detectedBssid = "AA:BB:CC:DD:EE:00", error = true),
            WifiRuleNetworks(previewState.currentWifiNetwork, previewState.scannedWifiNetworks, previewState.knownWifiNetworks),
            providers, "Example DNS", {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RuleTargetStepPreview() = QuickTileSettingsTheme {
    Surface(Modifier.fillMaxSize()) {
        WifiRuleEditorScreen(
            WifiRuleDraft(ssid = "Home"), WifiRuleNetworks(),
            providers + listOf(
                DnsHostnameEntry("adguard", "AdGuard DNS", "dns.adguard.com"),
                DnsHostnameEntry("cloudflare", "Cloudflare", "one.one.one.one"),
                DnsHostnameEntry("quad9", "Quad9 Security", "dns.quad9.net")),
            "Cloudflare", {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
            modifier = Modifier.padding(16.dp), targetStep = true)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AutomationDnsTargetPreview() = QuickTileSettingsTheme {
    Surface {
        AutomationDnsTargetScreen(DNS_MODE_AUTO, null,
            providers + DnsHostnameEntry("cloudflare", "Cloudflare", "one.one.one.one"),
            "", {}, { _, _ -> }, Modifier.padding(16.dp))
    }
}

private object PreviewActions : AutomationUiActions {
    override fun setVpnDetectionEnabled(enabled: Boolean) = Unit
    override fun setVpnDetectionMode(mode: String) = Unit
    override fun setNetworkTypeDetectionEnabled(enabled: Boolean) = Unit
    override fun setNetworkTypeDetectionMode(mode: String) = Unit
    override fun setDnsStateOnWifi(state: String) = Unit
    override fun setDnsHostnameOnWifi(hostname: String?) = Unit
    override fun setWifiNetworkRulesEnabled(enabled: Boolean) = Unit
    override fun addWifiNetworkRule(ssid: String?, bssid: String?, actionMode: String, dnsHostname: String?) = true
    override fun updateWifiNetworkRule(id: String, ssid: String?, bssid: String?, actionMode: String, dnsHostname: String?) = true
    override fun deleteWifiNetworkRule(ruleId: String) = Unit
    override fun setDnsStateOnMobile(state: String) = Unit
    override fun setDnsHostnameOnMobile(hostname: String?) = Unit
    override fun refreshWifiNetworks() = Unit
    override fun launchWifiSsidPermissionRequest() = Unit
}
