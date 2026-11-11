package com.rbn.qtsettings.feature.automation.presentation

/** Rendering snapshot; persisted modes and rule IDs retain their storage contract. */
data class AutomationUiState(
    val vpnDetectionEnabled: Boolean,
    val vpnDetectionMode: String,
    val networkTypeDetectionEnabled: Boolean,
    val networkTypeDetectionMode: String,
    val dnsStateOnWifi: String,
    val dnsHostnameOnWifi: String?,
    val wifiNetworkRulesEnabled: Boolean,
    val wifiNetworkRules: List<com.rbn.qtsettings.data.WifiDnsRule>,
    val knownWifiNetworks: List<com.rbn.qtsettings.data.KnownWifiNetwork>,
    val currentWifiNetwork: com.rbn.qtsettings.data.WifiNetworkIdentity?,
    val scannedWifiNetworks: List<com.rbn.qtsettings.data.WifiNetworkIdentity>,
    val isWifiResultsLoading: Boolean,
    val wifiIdentityAccessState: com.rbn.qtsettings.data.WifiIdentityAccessState,
    val hasBackgroundLocationPermission: Boolean,
    val dnsStateOnMobile: String,
    val dnsHostnameOnMobile: String?,
    val notificationsAllowed: Boolean = true
)
