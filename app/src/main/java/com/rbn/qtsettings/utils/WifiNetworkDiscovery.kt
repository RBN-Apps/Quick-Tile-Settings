package com.rbn.qtsettings.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.rbn.qtsettings.data.WifiNetworkIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WifiNetworkDiscovery {
    private const val TAG = "WifiNetworkDiscovery"

    /**
     * Returns the most recent results Android exposes. The platform performs Wi-Fi scans
     * independently; requesting an app-triggered scan would require the broad CHANGE_WIFI_STATE
     * permission and may be throttled or disabled by the user.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun loadLatestResults(
        context: Context
    ): List<WifiNetworkIdentity> = withContext(Dispatchers.IO) {
        if (!PermissionUtils.canAccessWifiSsid(context)) return@withContext emptyList()

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            wifiManager.scanResults
                .mapNotNull { result ->
                    @Suppress("DEPRECATION")
                    WifiNetworkRuleUtils.normalizeIdentity(
                        WifiNetworkIdentity(
                            ssid = result.SSID,
                            bssid = result.BSSID
                        )
                    )
                }
                .distinctBy(WifiNetworkIdentity::stableKey)
                .sortedWith(
                    compareBy<WifiNetworkIdentity> { it.ssid?.lowercase().orEmpty() }
                        .thenBy { it.bssid.orEmpty() }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read Wi-Fi scan results", e)
            emptyList()
        }
    }
}
