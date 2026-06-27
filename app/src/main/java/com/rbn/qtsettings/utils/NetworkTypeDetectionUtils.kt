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
    private val trackedNetworkCapabilities = mutableMapOf<Network, NetworkCapabilities>()
    private val trackedNetworkCapabilitiesLock = Any()
    private var registeredNetworkCallbackCount = 0

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

                getNetworkTypeFromCapabilities(networkCapabilities).let { activeType ->
                    if (activeType != NETWORK_TYPE_NONE) return activeType
                }
            }

            getTrackedNetworkType()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network type", e)
            NETWORK_TYPE_NONE
        }
    }

    internal fun getNetworkTypeFromCapabilities(networkCapabilities: NetworkCapabilities?): String {
        return when {
            networkCapabilities == null -> NETWORK_TYPE_NONE
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_TYPE_WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NETWORK_TYPE_MOBILE
            else -> NETWORK_TYPE_NONE
        }
    }

    internal fun getBestNetworkTypeFromCapabilities(
        networkCapabilities: Collection<NetworkCapabilities>
    ): String {
        var hasMobile = false

        for (capabilities in networkCapabilities) {
            when (getNetworkTypeFromCapabilities(capabilities)) {
                NETWORK_TYPE_WIFI -> return NETWORK_TYPE_WIFI
                NETWORK_TYPE_MOBILE -> hasMobile = true
            }
        }

        return if (hasMobile) NETWORK_TYPE_MOBILE else NETWORK_TYPE_NONE
    }

    private fun rememberNetworkCapabilities(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        synchronized(trackedNetworkCapabilitiesLock) {
            trackedNetworkCapabilities[network] = networkCapabilities
        }
    }

    private fun forgetNetwork(network: Network) {
        synchronized(trackedNetworkCapabilitiesLock) {
            trackedNetworkCapabilities.remove(network)
        }
    }

    private fun beginNetworkCallbackRegistration() {
        synchronized(trackedNetworkCapabilitiesLock) {
            if (registeredNetworkCallbackCount == 0) {
                trackedNetworkCapabilities.clear()
            }
            registeredNetworkCallbackCount++
        }
    }

    private fun endNetworkCallbackRegistration() {
        synchronized(trackedNetworkCapabilitiesLock) {
            registeredNetworkCallbackCount = (registeredNetworkCallbackCount - 1).coerceAtLeast(0)
            if (registeredNetworkCallbackCount == 0) {
                trackedNetworkCapabilities.clear()
            }
        }
    }

    private fun getTrackedNetworkType(): String {
        return synchronized(trackedNetworkCapabilitiesLock) {
            getBestNetworkTypeFromCapabilities(trackedNetworkCapabilities.values)
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
                // Wait for onCapabilitiesChanged; minSdk 29 guarantees it follows availability.
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                rememberNetworkCapabilities(network, networkCapabilities)
                checkNetworkType()
            }

            override fun onLost(network: Network) {
                forgetNetwork(network)
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
        var registrationStarted = false
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            beginNetworkCallbackRegistration()
            registrationStarted = true
            val request = NetworkRequest.Builder()
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            Log.d(TAG, "Network type callback registered")
        } catch (e: Exception) {
            if (registrationStarted) {
                endNetworkCallbackRegistration()
            }
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
            endNetworkCallbackRegistration()
            Log.d(TAG, "Network type callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network type callback", e)
        }
    }
}
