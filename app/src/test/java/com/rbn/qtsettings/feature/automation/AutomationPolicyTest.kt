package com.rbn.qtsettings.feature.automation

import com.rbn.qtsettings.feature.automation.domain.*
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import org.junit.Assert.*
import org.junit.Test

class AutomationPolicyTest {
    private val home = WifiSsid("Home")
    private val ap = WifiBssid("AA:BB:CC:DD:EE:FF")
    private val settings = AutomationSettings(true, MonitoringMode.Background, true,
        MonitoringMode.Background, true, emptyList(), DnsTarget.Automatic, DnsTarget.Off)
    private fun rule(id: String, ssid: WifiSsid?, bssid: WifiBssid?, action: WifiRuleAction) =
        AutomationRule(id, WifiIdentity(ssid, bssid), action)
    private fun resolve(rules: List<AutomationRule>, identity: WifiIdentity?) =
        AutomationPolicy.networkTarget(settings.copy(rules = rules), AutomationNetwork(NetworkKind.Wifi, identity))

    @Test fun bssidAndSsidWinsThenBssidThenSsid_regardlessOfRuleOrder() {
        val broad = rule("ssid", home, null, WifiRuleAction.Set(DnsTarget.Off))
        val accessPoint = rule("bssid", null, ap, WifiRuleAction.Set(DnsTarget.Hostname("ap.example")))
        val exact = rule("both", home, ap, WifiRuleAction.Set(DnsTarget.Hostname("exact.example")))
        for (rules in listOf(listOf(exact, accessPoint, broad), listOf(broad, accessPoint, exact))) {
            assertEquals(DnsTarget.Hostname("exact.example"), resolve(rules, WifiIdentity(home, ap)))
            assertEquals(DnsTarget.Hostname("ap.example"), resolve(rules, WifiIdentity(WifiSsid("Other"), ap)))
            assertEquals(DnsTarget.Off, resolve(rules, WifiIdentity(home, null)))
        }
    }

    @Test fun explicitDefaultExceptionUsesWifiDefault_notLessSpecificRule() {
        val rules = listOf(rule("home", home, null, WifiRuleAction.Set(DnsTarget.Off)),
            rule("exception", home, ap, WifiRuleAction.Default))
        assertEquals(DnsTarget.Automatic, resolve(rules, WifiIdentity(home, ap)))
    }

    @Test fun identityRedactionDoesNotGuess_bssidOnlyStillMatches() {
        val rules = listOf(rule("ap", null, ap, WifiRuleAction.Set(DnsTarget.Off)))
        assertNull(resolve(rules, null))
        assertNull(resolve(rules, WifiIdentity()))
        assertEquals(DnsTarget.Off, resolve(rules, WifiIdentity(bssid = ap)))
        assertEquals(DnsTarget.Automatic, resolve(rules, WifiIdentity(ssid = home)))
        assertEquals(DnsTarget.Automatic, resolve(emptyList(), null))
        assertEquals(DnsTarget.Automatic, AutomationPolicy.networkTarget(
            settings.copy(wifiRulesEnabled = false, rules = rules), AutomationNetwork(NetworkKind.Wifi)))
    }

    @Test fun noNetworkAndOtherLeaveDnsUntouched_mobileUsesItsOwnDefault() {
        assertNull(AutomationPolicy.networkTarget(settings, AutomationNetwork(NetworkKind.None)))
        assertNull(AutomationPolicy.networkTarget(settings, AutomationNetwork(NetworkKind.Other)))
        assertEquals(DnsTarget.Off, AutomationPolicy.networkTarget(settings, AutomationNetwork(NetworkKind.Mobile)))
    }

    @Test fun duplicateNetworkDoesNotOverrideManualAction_vpnDisconnectAndForcedRestartDoReevaluate() {
        val transitions = NetworkTransitionPolicy()
        val network = AutomationNetwork(NetworkKind.Wifi, WifiIdentity(home, ap))
        assertTrue(transitions.accept(network, false))
        assertFalse(transitions.accept(network, false))
        assertTrue(transitions.accept(network, true))
        assertFalse(transitions.accept(network, true))
        assertTrue(transitions.accept(network, false))
        assertTrue(transitions.accept(network, false, force = true))
        assertFalse(transitions.accept(network, false))
    }
}
