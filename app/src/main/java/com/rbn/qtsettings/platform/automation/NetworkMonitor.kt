package com.rbn.qtsettings.platform.automation

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.data.toAutomationNetwork
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.feature.automation.domain.NetworkTransitionPolicy
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.platform.automation.NetworkDnsAutomation
import com.rbn.qtsettings.platform.automation.NetworkTypeDetectionUtils
import com.rbn.qtsettings.platform.automation.VpnDetectionUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val prefsManager: AutomationSettingsSource,
    private val onStateChanged: () -> Unit
) {
    @Volatile private var generation = 0L
    private val transitions = NetworkTransitionPolicy()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var serviceJob: Job? = null
    private var wifiIdentityFallbackJob: Job? = null
    var currentNetworkState = DetectedNetworkState()
        private set
    var monitorWifiSsid = false
        private set
    private var initialNetworkInventoryPending = false
    private var forceNextResolvedState = false
    private var vpnOverrideWasActive = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun startNetworkMonitoring() {
        stopNetworkMonitoring()
        monitorWifiSsid = prefsManager.areWifiNetworkRulesEnabled()
        initialNetworkInventoryPending = monitorWifiSsid
        forceNextResolvedState = true
        registerNetworkCallback()

        val detectedState = NetworkTypeDetectionUtils.getCurrentNetworkState(
            context = context,
            includeWifiSsid = monitorWifiSsid
        )
        handleNetworkStateChange(detectedState)

        serviceJob = serviceScope.launch {
            while (isActive) {
                try {
                    delay(CHECK_INTERVAL_MS.milliseconds)
                    checkNetworkState()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic network type check", e)
                    delay(CHECK_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val registeredGeneration = ++generation
            networkCallback = NetworkTypeDetectionUtils.createNetworkStateCallback(
                context = context,
                includeWifiSsid = monitorWifiSsid,
                isCurrent = { registeredGeneration == generation },
                onNetworkStateChanged = { newNetworkState ->
                    serviceScope.launch {
                        if (registeredGeneration == generation) handleNetworkStateChange(newNetworkState)
                    }
                }
            )

            networkCallback?.let { callback ->
                NetworkTypeDetectionUtils.registerNetworkTypeCallback(context, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun checkNetworkState() {
        handleNetworkStateChange(
            NetworkTypeDetectionUtils.getCurrentNetworkState(
                context = context,
                includeWifiSsid = monitorWifiSsid
            )
        )
    }

    private fun handleNetworkStateChange(
        newNetworkState: DetectedNetworkState,
        force: Boolean = false
    ) {
        val vpnOverrideActive =
            prefsManager.isVpnDetectionEnabled() && VpnDetectionUtils.isVpnActive(context)
        val vpnOverrideChanged = vpnOverrideActive != vpnOverrideWasActive
        if (initialNetworkInventoryPending && !force) {
            if (newNetworkState.networkType == NETWORK_TYPE_WIFI &&
                newNetworkState.wifiSsid != null
            ) {
                initialNetworkInventoryPending = false
            } else {
                scheduleWifiIdentityFallback()
                return
            }
        }
        initialNetworkInventoryPending = false

        if (monitorWifiSsid &&
            !force &&
            newNetworkState.networkType == NETWORK_TYPE_WIFI &&
            newNetworkState.wifiSsid == null
        ) {
            if (newNetworkState != currentNetworkState || vpnOverrideChanged) {
                scheduleWifiIdentityFallback()
            }
            return
        }
        wifiIdentityFallbackJob?.cancel()
        wifiIdentityFallbackJob = null

        val applyForced = force || forceNextResolvedState
        forceNextResolvedState = false
        if (!transitions.accept(newNetworkState.toAutomationNetwork(), vpnOverrideActive, applyForced)) return

        val oldNetworkState = currentNetworkState
        currentNetworkState = newNetworkState
        vpnOverrideWasActive = vpnOverrideActive

        if (newNetworkState.networkType == NETWORK_TYPE_WIFI) {
            prefsManager.recordKnownWifiNetwork(
                WifiNetworkIdentity(
                    ssid = newNetworkState.wifiSsid,
                    bssid = newNetworkState.wifiBssid
                )
            )
        }

        Log.i(
            TAG,
            "Network state changed from ${oldNetworkState.networkType} to " +
                    "${newNetworkState.networkType}; Wi-Fi identity available=" +
                    (newNetworkState.wifiSsid != null)
        )

        if (vpnOverrideActive) {
            Log.d(TAG, "VPN is active and VPN detection is enabled, skipping DNS change")
            onStateChanged()
            return
        }

        NetworkDnsAutomation.apply(context, prefsManager, newNetworkState)

        onStateChanged()
    }

    private fun scheduleWifiIdentityFallback() {
        if (wifiIdentityFallbackJob?.isActive == true) return
        Log.d(TAG, "Waiting briefly for location-aware Wi-Fi capabilities")
        wifiIdentityFallbackJob = serviceScope.launch {
            delay(CHECK_INTERVAL_MS.milliseconds)
            wifiIdentityFallbackJob = null
            handleNetworkStateChange(
                NetworkTypeDetectionUtils.getCurrentNetworkState(
                    context = context,
                    includeWifiSsid = true
                ),
                force = true
            )
        }
    }

    fun stopNetworkMonitoring() {
        generation++
        serviceJob?.cancel()
        serviceJob = null
        wifiIdentityFallbackJob?.cancel()
        wifiIdentityFallbackJob = null
        initialNetworkInventoryPending = false
        forceNextResolvedState = false
        networkCallback?.let { callback ->
            NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(context, callback)
        }
        networkCallback = null
    }

    fun refreshRedactedIdentity() {
        if (monitorWifiSsid && currentNetworkState.networkType == NETWORK_TYPE_WIFI &&
            currentNetworkState.wifiSsid == null && currentNetworkState.wifiBssid == null) {
            initialNetworkInventoryPending = true
            networkCallback?.let { NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(context, it) }
            registerNetworkCallback()
        }
        onStateChanged()
    }
    fun close() {
        stopNetworkMonitoring()
        serviceScope.cancel()
    }
    val running get() = serviceJob?.isActive == true
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val CHECK_INTERVAL_MS = 3000L
    }
}
