package com.rbn.qtsettings.feature.automation.presentation

/** UI callbacks expose intents without depending on a ViewModel or Android lifecycle. */
interface AutomationUiActions {
    fun setVpnDetectionEnabled(enabled: Boolean): Unit
    fun setVpnDetectionMode(mode: String): Unit
    fun setNetworkTypeDetectionEnabled(enabled: Boolean): Unit
    fun setNetworkTypeDetectionMode(mode: String): Unit
    fun setDnsStateOnWifi(state: String): Unit
    fun setDnsHostnameOnWifi(hostname: String?): Unit
    fun setWifiNetworkRulesEnabled(enabled: Boolean): Unit
    fun addWifiNetworkRule(
        ssid: String?,
        bssid: String?,
        actionMode: String,
        dnsHostname: String?
    ): Boolean
    fun updateWifiNetworkRule(
        id: String,
        ssid: String?,
        bssid: String?,
        actionMode: String,
        dnsHostname: String?
    ): Boolean
    fun deleteWifiNetworkRule(ruleId: String): Unit
    fun setDnsStateOnMobile(state: String): Unit
    fun setDnsHostnameOnMobile(hostname: String?): Unit
    fun refreshWifiNetworks(): Unit
    fun launchWifiSsidPermissionRequest(): Unit
}
