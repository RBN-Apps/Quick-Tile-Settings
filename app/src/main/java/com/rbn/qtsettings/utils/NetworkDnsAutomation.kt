package com.rbn.qtsettings.utils

import android.content.Context
import android.util.Log
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI

object NetworkDnsAutomation {
    private const val TAG = "NetworkDnsAutomation"

    fun resolveTarget(
        preferencesManager: PreferencesManager,
        networkState: DetectedNetworkState
    ): PrivateDnsTarget? {
        return when (networkState.networkType) {
            NETWORK_TYPE_WIFI -> WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = preferencesManager.areWifiNetworkRulesEnabled(),
                rules = preferencesManager.getWifiNetworkRules(),
                currentNetwork = WifiNetworkIdentity(
                    ssid = networkState.wifiSsid,
                    bssid = networkState.wifiBssid
                ),
                defaultTarget = PrivateDnsTarget(
                    mode = preferencesManager.getDnsStateOnWifi(),
                    hostname = preferencesManager.getDnsHostnameOnWifi()
                )
            )

            NETWORK_TYPE_MOBILE -> PrivateDnsTarget(
                mode = preferencesManager.getDnsStateOnMobile(),
                hostname = preferencesManager.getDnsHostnameOnMobile()
            )

            else -> null
        }
    }

    fun apply(
        context: Context,
        preferencesManager: PreferencesManager,
        networkState: DetectedNetworkState
    ): Boolean {
        val target = resolveTarget(preferencesManager, networkState) ?: run {
            Log.d(TAG, "No safe DNS target for the current network; DNS settings unchanged")
            return false
        }
        return NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
            context = context,
            networkType = networkState.networkType,
            dnsMode = target.mode,
            dnsHostname = target.hostname
        )
    }
}
