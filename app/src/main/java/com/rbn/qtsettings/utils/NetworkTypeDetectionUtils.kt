package com.rbn.qtsettings.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.WifiNetworkIdentity
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
    fun getCurrentNetworkType(
        context: Context,
        preferTrackedWifi: Boolean = false
    ): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            // A Wi-Fi network that Private DNS prevents from validating may not be Android's
            // default network yet. Prefer a connected Wi-Fi transport observed by the callback so
            // its SSID rule can turn DNS off and allow validation to complete.
            val activeNetwork = connectivityManager.activeNetwork
            val activeCapabilities = activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            resolveCurrentNetworkType(
                activeCapabilities = activeCapabilities,
                trackedCapabilities = synchronized(trackedNetworkCapabilitiesLock) {
                    trackedNetworkCapabilities.values.toList()
                },
                preferTrackedWifi = preferTrackedWifi
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network type", e)
            NETWORK_TYPE_NONE
        }
    }

    /**
     * Returns the physical network type together with the connected Wi-Fi SSID when Android allows
     * access to it. On Android 12+ the SSID normally comes from capabilities delivered to a
     * location-aware [ConnectivityManager.NetworkCallback], which are retained in this object.
     */
    fun getCurrentNetworkState(
        context: Context,
        includeWifiSsid: Boolean = true
    ): DetectedNetworkState {
        val networkType = getCurrentNetworkType(
            context = context,
            preferTrackedWifi = includeWifiSsid
        )
        val wifiIdentity = if (includeWifiSsid && networkType == NETWORK_TYPE_WIFI) {
            getCurrentWifiIdentity(context)
        } else {
            null
        }
        return DetectedNetworkState(
            networkType = networkType,
            wifiSsid = wifiIdentity?.ssid,
            wifiBssid = wifiIdentity?.bssid
        )
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

    internal fun resolveCurrentNetworkType(
        activeCapabilities: NetworkCapabilities?,
        trackedCapabilities: Collection<NetworkCapabilities>,
        preferTrackedWifi: Boolean
    ): String {
        val trackedType = getBestNetworkTypeFromCapabilities(trackedCapabilities)
        if (preferTrackedWifi && trackedType == NETWORK_TYPE_WIFI) return NETWORK_TYPE_WIFI

        val activeType = getNetworkTypeFromCapabilities(activeCapabilities)
        return if (activeType != NETWORK_TYPE_NONE) activeType else trackedType
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

    @SuppressLint("MissingPermission")
    private fun getCurrentWifiIdentity(context: Context): WifiNetworkIdentity? {
        if (!PermissionUtils.canAccessWifiSsid(context)) {
            return null
        }

        // Android 10/11 do not attach WifiInfo to NetworkCapabilities yet.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return getLegacyWifiIdentity(context)
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = try {
            connectivityManager.activeNetwork
        } catch (e: Exception) {
            Log.w(TAG, "Unable to identify the active network", e)
            null
        }
        val trackedActiveSsid = synchronized(trackedNetworkCapabilitiesLock) {
            activeNetwork
                ?.let(trackedNetworkCapabilities::get)
                ?.let(::getWifiIdentityFromCapabilities)
        }
        if (trackedActiveSsid != null) return trackedActiveSsid

        val activeSsid = try {
            activeNetwork
                ?.let(connectivityManager::getNetworkCapabilities)
                ?.let(::getWifiIdentityFromCapabilities)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect active Wi-Fi capabilities", e)
            null
        }
        if (activeSsid != null) return activeSsid

        return synchronized(trackedNetworkCapabilitiesLock) {
            trackedNetworkCapabilities.values
                .asSequence()
                .filter {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
                .mapNotNull(::getWifiIdentityFromCapabilities)
                .firstOrNull()
        }
    }

    private fun getWifiIdentityFromCapabilities(
        capabilities: NetworkCapabilities
    ): WifiNetworkIdentity? {
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return null
        return getWifiIdentity(wifiInfo)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getLegacyWifiIdentity(context: Context): WifiNetworkIdentity? = try {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        getWifiIdentity(wifiManager.connectionInfo)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read the connected Wi-Fi identity", e)
        null
    }

    private fun getWifiIdentity(wifiInfo: WifiInfo): WifiNetworkIdentity? {
        return try {
            WifiNetworkRuleUtils.normalizeIdentity(
                WifiNetworkIdentity(
                    ssid = WifiNetworkRuleUtils.normalizeDetectedSsid(wifiInfo.ssid),
                    bssid = wifiInfo.bssid
                )
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Wi-Fi identity is unavailable without location access")
            null
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
            val saved = when (dnsMode) {
                DNS_MODE_OFF -> {
                    val result = Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_OFF
                    )
                    Log.i(TAG, "Set Private DNS to OFF for network type: $networkType")
                    result
                }

                DNS_MODE_AUTO -> {
                    val result = Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_AUTO
                    )
                    Log.i(TAG, "Set Private DNS to AUTO for network type: $networkType")
                    result
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
                        val hostnameSaved = Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_SPECIFIER,
                            dnsHostname
                        )
                        val modeSaved = Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_ON
                        )
                        Log.i(
                            TAG,
                            "Set Private DNS to hostname '$dnsHostname' for network type: $networkType"
                        )
                        hostnameSaved && modeSaved
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
            if (!saved) {
                Log.e(TAG, "Android rejected the Private DNS update for network type: $networkType")
            }
            saved
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Private DNS for network type: $networkType", e)
            false
        }
    }

    /** Creates a callback that also reacts to Wi-Fi-to-Wi-Fi SSID changes. */
    fun createNetworkStateCallback(
        context: Context,
        includeWifiSsid: Boolean = true,
        onNetworkStateChanged: (DetectedNetworkState) -> Unit
    ): ConnectivityManager.NetworkCallback {
        val callbackEvents = NetworkStateCallbackEvents(
            context = context,
            includeWifiSsid = includeWifiSsid,
            onNetworkStateChanged = onNetworkStateChanged
        )
        return if (includeWifiSsid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createLocationAwareCallback(callbackEvents)
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) = callbackEvents.onCapabilitiesChanged(network, networkCapabilities)

                override fun onLost(network: Network) = callbackEvents.onLost(network)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createLocationAwareCallback(
        callbackEvents: NetworkStateCallbackEvents
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = callbackEvents.onCapabilitiesChanged(network, networkCapabilities)

            override fun onLost(network: Network) = callbackEvents.onLost(network)
        }
    }

    private class NetworkStateCallbackEvents(
        private val context: Context,
        private val includeWifiSsid: Boolean,
        private val onNetworkStateChanged: (DetectedNetworkState) -> Unit
    ) {
        private var lastNetworkState: DetectedNetworkState? = null

        fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            rememberNetworkCapabilities(network, networkCapabilities)
            publishIfChanged()
        }

        fun onLost(network: Network) {
            forgetNetwork(network)
            publishIfChanged()
        }

        private fun publishIfChanged() {
            val currentState = getCurrentNetworkState(context, includeWifiSsid)
            if (currentState != lastNetworkState) {
                lastNetworkState = currentState
                Log.d(TAG, "Network state changed to type=${currentState.networkType}")
                onNetworkStateChanged(currentState)
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
