package com.rbn.qtsettings.feature.automation.domain

import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import kotlinx.coroutines.flow.StateFlow

/** Settings boundary; legacy DTO fields retain their serialized representation. */
interface AutomationSettingsSource {
    val vpnDetectionEnabled: StateFlow<Boolean>
    val vpnDetectionMode: StateFlow<String>
    val networkTypeDetectionEnabled: StateFlow<Boolean>
    val networkTypeDetectionMode: StateFlow<String>
    val dnsStateOnWifi: StateFlow<String>
    val dnsHostnameOnWifi: StateFlow<String?>
    val wifiNetworkRulesEnabled: StateFlow<Boolean>
    val wifiNetworkRules: StateFlow<List<WifiDnsRule>>
    val knownWifiNetworks: StateFlow<List<KnownWifiNetwork>>
    val dnsStateOnMobile: StateFlow<String>
    val dnsHostnameOnMobile: StateFlow<String?>

    fun setVpnDetectionEnabled(enabled: Boolean): Unit
    fun setVpnDetectionMode(mode: String): Unit
    fun setNetworkTypeDetectionEnabled(enabled: Boolean): Unit
    fun setNetworkTypeDetectionMode(mode: String): Unit
    fun setDnsStateOnWifi(state: String): Unit
    fun setDnsHostnameOnWifi(hostname: String?): Unit
    fun setWifiNetworkRulesEnabled(enabled: Boolean): Unit
    fun addWifiNetworkRule(rule: WifiDnsRule): Boolean
    fun updateWifiNetworkRule(rule: WifiDnsRule): Boolean
    fun deleteWifiNetworkRule(ruleId: String): Unit
    fun recordKnownWifiNetwork(
        identity: WifiNetworkIdentity,
        seenAtEpochMillis: Long = System.currentTimeMillis()
    ): Unit
    fun setDnsStateOnMobile(state: String): Unit
    fun setDnsHostnameOnMobile(hostname: String?): Unit
    fun isVpnDetectionEnabled(): Boolean
    fun getVpnDetectionMode(): String
    fun isNetworkTypeDetectionEnabled(): Boolean
    fun getNetworkTypeDetectionMode(): String
    fun getDnsStateOnWifi(): String
    fun getDnsHostnameOnWifi(): String?
    fun areWifiNetworkRulesEnabled(): Boolean
    fun getWifiNetworkRules(): List<WifiDnsRule>
    fun getKnownWifiNetworks(): List<KnownWifiNetwork>
    fun getDnsStateOnMobile(): String
    fun getDnsHostnameOnMobile(): String?
}
