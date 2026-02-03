package com.rbn.qtsettings.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.util.Log
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER

object NetworkTypeDetectionUtils {
    private const val TAG = "NetworkTypeDetection"

    /**
     * Gets the current active network type
     * @return NETWORK_TYPE_WIFI, NETWORK_TYPE_MOBILE, or NETWORK_TYPE_NONE
     */
    fun getCurrentNetworkType(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork)

                if (networkCapabilities != null) {
                    return when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_TYPE_WIFI
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NETWORK_TYPE_MOBILE
                        else -> NETWORK_TYPE_NONE
                    }
                }
            }
            NETWORK_TYPE_NONE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network type", e)
            NETWORK_TYPE_NONE
        }
    }

    /**
     * Applies the configured DNS settings based on the network type
     * @param context Application context
     * @param networkType Current network type (NETWORK_TYPE_WIFI or NETWORK_TYPE_MOBILE)
     * @param dnsMode The desired DNS mode (off/auto/hostname)
     * @param dnsHostname The DNS hostname (only used if dnsMode is "hostname")
     * @return true if successfully applied
     */
    fun setPrivateDnsForNetworkType(
        context: Context,
        networkType: String,
        dnsMode: String,
        dnsHostname: String?
    ): Boolean {
        // Don't apply if VPN is active (VPN takes priority)
        if (VpnDetectionUtils.isVpnActive(context)) {
            Log.d(TAG, "VPN is active, skipping network type DNS change")
            return false
        }

        return try {
            when (dnsMode) {
                DNS_MODE_OFF -> {
                    Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_OFF
                    )
                    Log.i(TAG, "Set Private DNS to OFF for network type: $networkType")
                }

                DNS_MODE_AUTO -> {
                    Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_AUTO
                    )
                    Log.i(TAG, "Set Private DNS to AUTO for network type: $networkType")
                }

                DNS_MODE_ON -> {
                    if (dnsHostname.isNullOrBlank()) {
                        Log.w(TAG, "Hostname mode requested but no hostname provided, using AUTO")
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_AUTO
                        )
                    } else {
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_ON
                        )
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_SPECIFIER,
                            dnsHostname
                        )
                        Log.i(
                            TAG,
                            "Set Private DNS to hostname '$dnsHostname' for network type: $networkType"
                        )
                    }
                }

                else -> {
                    Log.w(TAG, "Unknown DNS mode: $dnsMode, using AUTO")
                    Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_AUTO
                    )
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Private DNS for network type: $networkType", e)
            false
        }
    }

    /**
     * Creates a network callback for monitoring network type changes
     */
    fun createNetworkTypeCallback(
        context: Context,
        onNetworkTypeChanged: (String) -> Unit
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            private var lastNetworkType: String? = null

            override fun onAvailable(network: Network) {
                checkNetworkType()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                checkNetworkType()
            }

            override fun onLost(network: Network) {
                checkNetworkType()
            }

            private fun checkNetworkType() {
                val currentType = getCurrentNetworkType(context)
                if (currentType != lastNetworkType) {
                    lastNetworkType = currentType
                    Log.d(TAG, "Network type changed to: $currentType")
                    onNetworkTypeChanged(currentType)
                }
            }
        }
    }

    /**
     * Registers the network callback to monitor all network changes
     */
    fun registerNetworkTypeCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback
    ) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            Log.d(TAG, "Network type callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network type callback", e)
        }
    }

    /**
     * Unregisters the network callback
     */
    fun unregisterNetworkTypeCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback
    ) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(callback)
            Log.d(TAG, "Network type callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network type callback", e)
        }
    }
}
