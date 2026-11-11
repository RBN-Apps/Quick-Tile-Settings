package com.rbn.qtsettings.platform.automation

import android.content.Context
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.domain.WifiDiscoverySource
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.platform.automation.NetworkTypeDetectionUtils
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import com.rbn.qtsettings.platform.automation.WifiNetworkDiscovery
import com.rbn.qtsettings.utils.WifiNetworkRuleUtils

class AndroidWifiDiscoverySource(context: Context) : WifiDiscoverySource {
    private val context = context.applicationContext
    override fun accessState() = PermissionUtils.getWifiIdentityAccessState(context)
    override fun hasPreciseLocationPermission() = PermissionUtils.hasPreciseLocationPermission(context)
    override fun hasBackgroundLocationPermission() = PermissionUtils.hasBackgroundLocationPermission(context)
    override fun current(): WifiNetworkIdentity? {
        val state = NetworkTypeDetectionUtils.getCurrentNetworkState(context, includeWifiSsid = true)
        return if (state.networkType == NETWORK_TYPE_WIFI)
            WifiNetworkRuleUtils.normalizeIdentity(WifiNetworkIdentity(state.wifiSsid, state.wifiBssid))
        else null
    }
    override suspend fun scanResults() = WifiNetworkDiscovery.loadLatestResults(context)
}
