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
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER

object VpnDetectionUtils {
    private const val TAG = "VpnDetectionUtils"

    fun isVpnActive(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork)
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN status", e)
            false
        }
    }

    fun getCurrentPrivateDnsMode(context: Context): String {
        return try {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE) ?: DNS_MODE_OFF
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current Private DNS mode", e)
            DNS_MODE_OFF
        }
    }

    fun getCurrentPrivateDnsHostname(context: Context): String? {
        return try {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current Private DNS hostname", e)
            null
        }
    }

    fun setPrivateDnsOff(context: Context): Boolean {
        return try {
            Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_OFF)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Private DNS to off", e)
            false
        }
    }

    fun restorePrivateDns(context: Context, mode: String, hostname: String? = null): Boolean {
        return try {
            when (mode) {
                DNS_MODE_OFF -> {
                    Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_OFF
                    )
                }

                DNS_MODE_ON -> {
                    if (hostname != null) {
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_ON
                        )
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_SPECIFIER,
                            hostname
                        )
                    } else {
                        Settings.Global.putString(
                            context.contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_AUTO
                        )
                    }
                }

                else -> {
                    Settings.Global.putString(
                        context.contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_AUTO
                    )
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring Private DNS", e)
            false
        }
    }

    fun createVpnNetworkCallback(
        onVpnConnected: () -> Unit,
        onVpnDisconnected: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // This will be called when any network becomes available
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    Log.d(TAG, "VPN network detected")
                    onVpnConnected()
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost, checking remaining VPN status")
                onVpnDisconnected()
            }
        }
    }

    fun registerVpnCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback
    ) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            Log.d(TAG, "VPN network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering VPN network callback", e)
        }
    }

    fun unregisterVpnCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback
    ) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(callback)
            Log.d(TAG, "VPN network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering VPN network callback", e)
        }
    }
}