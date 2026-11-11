package com.rbn.qtsettings.feature.automation.data

import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.domain.*
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.WifiNetworkRuleUtils

fun String.toMonitoringMode() = when (this) {
    Constants.TILE_ONLY_DETECTION -> MonitoringMode.TileOnly
    Constants.BACKGROUND_DETECTION -> MonitoringMode.Background
    else -> MonitoringMode.Unknown
}

/** Mirrors the platform writer's fallback for unknown modes or a missing hostname. */
internal fun automationTarget(mode: String, hostname: String?): DnsTarget = when {
    mode == Constants.DNS_MODE_OFF -> DnsTarget.Off
    mode == Constants.DNS_MODE_ON && !hostname.isNullOrBlank() -> DnsTarget.Hostname(hostname)
    else -> DnsTarget.Automatic
}
fun WifiNetworkIdentity.toAutomationIdentity(): WifiIdentity? =
    WifiNetworkRuleUtils.normalizeIdentity(this)?.let { WifiIdentity(it.ssid?.let(::WifiSsid), it.bssid?.let(::WifiBssid)) }
fun DetectedNetworkState.toAutomationNetwork() = AutomationNetwork(
    when (networkType) {
        Constants.NETWORK_TYPE_WIFI -> NetworkKind.Wifi
        Constants.NETWORK_TYPE_MOBILE -> NetworkKind.Mobile
        Constants.NETWORK_TYPE_NONE -> NetworkKind.None
        else -> NetworkKind.Other
    },
    WifiNetworkIdentity(wifiSsid, wifiBssid).toAutomationIdentity()
)
fun AutomationSettingsSource.readSettings() = AutomationSettings(
    isVpnDetectionEnabled(), getVpnDetectionMode().toMonitoringMode(),
    isNetworkTypeDetectionEnabled(), getNetworkTypeDetectionMode().toMonitoringMode(),
    areWifiNetworkRulesEnabled(),
    getWifiNetworkRules().mapNotNull { it.toAutomationRule() },
    automationTarget(getDnsStateOnWifi(), getDnsHostnameOnWifi()),
    automationTarget(getDnsStateOnMobile(), getDnsHostnameOnMobile())
)

internal fun WifiDnsRule.toAutomationRule(): AutomationRule? =
    WifiNetworkRuleUtils.normalizeRule(this)?.let { rule ->
        val matcher = WifiNetworkIdentity(rule.ssid, rule.bssid).toAutomationIdentity() ?: return null
        AutomationRule(
            id = rule.id,
            matcher = matcher,
            action = if (rule.actionMode == Constants.WIFI_DNS_ACTION_DEFAULT) WifiRuleAction.Default
                else WifiRuleAction.Set(automationTarget(rule.actionMode, rule.dnsHostname))
        )
    }
