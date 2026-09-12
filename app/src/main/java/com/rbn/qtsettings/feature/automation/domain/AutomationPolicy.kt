package com.rbn.qtsettings.feature.automation.domain

import com.rbn.qtsettings.feature.dns.domain.DnsTarget

@JvmInline value class WifiSsid(val value: String)
@JvmInline value class WifiBssid(val value: String)
data class WifiIdentity(val ssid: WifiSsid? = null, val bssid: WifiBssid? = null)
enum class NetworkKind { Wifi, Mobile, None, Other }
data class AutomationNetwork(val kind: NetworkKind, val wifi: WifiIdentity? = null)
enum class MonitoringMode { TileOnly, Background, Unknown }
sealed interface WifiRuleAction {
    data object Default : WifiRuleAction
    data class Set(val target: DnsTarget) : WifiRuleAction
}
data class AutomationRule(val id: String, val matcher: WifiIdentity, val action: WifiRuleAction)
data class AutomationSettings(
    val vpnEnabled: Boolean,
    val vpnMode: MonitoringMode,
    val networkEnabled: Boolean,
    val networkMode: MonitoringMode,
    val wifiRulesEnabled: Boolean,
    val rules: List<AutomationRule>,
    val wifiDefault: DnsTarget,
    val mobileDefault: DnsTarget
)

/** Baseline priorities. Unknown/redacted identities never guess a matching rule. */
object AutomationPolicy {
    fun networkTarget(settings: AutomationSettings, network: AutomationNetwork): DnsTarget? =
        when (network.kind) {
            NetworkKind.Mobile -> settings.mobileDefault
            NetworkKind.Wifi -> wifiTarget(settings.wifiRulesEnabled, settings.rules, network.wifi, settings.wifiDefault)
            NetworkKind.None, NetworkKind.Other -> null
        }

    fun wifiTarget(
        rulesEnabled: Boolean,
        rules: List<AutomationRule>,
        identity: WifiIdentity?,
        defaultTarget: DnsTarget
    ): DnsTarget? {
        if (!rulesEnabled || rules.isEmpty()) return defaultTarget
        if (identity == null || (identity.ssid == null && identity.bssid == null)) return null
        val rule = rules.filter {
            (it.matcher.ssid == null || it.matcher.ssid == identity.ssid) &&
                (it.matcher.bssid == null || it.matcher.bssid == identity.bssid)
        }.maxByOrNull {
            (if (it.matcher.ssid != null) 1 else 0) + (if (it.matcher.bssid != null) 2 else 0)
        }
        return when (val action = rule?.action) {
            is WifiRuleAction.Set -> action.target
            else -> defaultTarget
        }
    }
}

/** Duplicates do not undo manual DNS actions; a real network/VPN transition still applies policy. */
class NetworkTransitionPolicy {
    private var previous: AutomationNetwork? = null
    private var previousVpn = false
    fun accept(network: AutomationNetwork, vpn: Boolean, force: Boolean = false): Boolean {
        if (!force && previous == network && previousVpn == vpn) return false
        previous = network
        previousVpn = vpn
        return true
    }
}
