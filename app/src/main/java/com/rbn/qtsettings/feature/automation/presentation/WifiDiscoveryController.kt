package com.rbn.qtsettings.feature.automation.presentation

import android.content.Context
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.feature.automation.domain.WifiDiscoverySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WifiDiscoveryController(
    private val source: () -> WifiDiscoverySource?,
    private val getCurrentContext: () -> Context?,
    private val prefsManager: AutomationSettingsSource,
    private val viewModelScope: CoroutineScope,
    private val _permissionGrantStatus: MutableStateFlow<String?>,
    private val reevaluateNetworkAutomation: () -> Unit
) {
    private val _requestWifiSsidPermission = MutableStateFlow(0)
    val requestWifiSsidPermission = _requestWifiSsidPermission.asStateFlow()

    private val _wifiIdentityAccessState = MutableStateFlow(
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
    )
    val wifiIdentityAccessState = _wifiIdentityAccessState.asStateFlow()

    private val _hasBackgroundLocationPermission = MutableStateFlow(false)
    val hasBackgroundLocationPermission = _hasBackgroundLocationPermission.asStateFlow()

    private val _currentWifiNetwork = MutableStateFlow<WifiNetworkIdentity?>(null)
    val currentWifiNetwork = _currentWifiNetwork.asStateFlow()

    private val _scannedWifiNetworks = MutableStateFlow<List<WifiNetworkIdentity>>(emptyList())
    val scannedWifiNetworks = _scannedWifiNetworks.asStateFlow()

    private val _isWifiResultsLoading = MutableStateFlow(false)
    val isWifiResultsLoading = _isWifiResultsLoading.asStateFlow()

    fun refreshWifiNetworks() {
        if (_isWifiResultsLoading.value) return
        if (getCurrentContext() == null) return
        if (!(source()?.accessState() == WifiIdentityAccessState.AVAILABLE)) {
            launchWifiSsidPermissionRequest()
            return
        }

        viewModelScope.launch {
            _isWifiResultsLoading.value = true
            try {
                refreshCurrentWifiNetwork()
                val results = source()?.scanResults().orEmpty()
                _scannedWifiNetworks.value = if (source()?.accessState() == WifiIdentityAccessState.AVAILABLE) results else emptyList()
            } finally {
                _isWifiResultsLoading.value = false
            }
        }
    }

    fun launchWifiSsidPermissionRequest() {
        val context = getCurrentContext()
        if (context != null && (source()?.hasPreciseLocationPermission() == true)) {
            _wifiIdentityAccessState.value = (source()?.accessState() ?: WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED)
            if (_wifiIdentityAccessState.value == WifiIdentityAccessState.AVAILABLE) {
                reevaluateNetworkAutomation()
                refreshWifiNetworks()
            } else {
                _permissionGrantStatus.value =
                    context.getString(R.string.wifi_location_services_disabled)
            }
            return
        }
        _requestWifiSsidPermission.value = _requestWifiSsidPermission.value + 1
    }

    fun onWifiSsidPermissionResult(granted: Boolean) {
        _requestWifiSsidPermission.value = 0
        val context = getCurrentContext()
        _wifiIdentityAccessState.value = if (context != null) {
            (source()?.accessState() ?: WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED)
        } else {
            WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
        }
        if (_wifiIdentityAccessState.value == WifiIdentityAccessState.AVAILABLE) {
            reevaluateNetworkAutomation()
            refreshWifiNetworks()
        } else if (context != null) {
            _permissionGrantStatus.value = context.getString(
                if (granted && (source()?.hasPreciseLocationPermission() == true)) {
                    R.string.wifi_location_services_disabled
                } else {
                    R.string.wifi_ssid_permission_denied
                }
            )
        }
    }

    fun clearWifiSsidPermissionRequest() {
        _requestWifiSsidPermission.value = 0
    }

    fun refreshSystemStates() {
        if (getCurrentContext() == null) return
        _wifiIdentityAccessState.value = (source()?.accessState() ?: WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED)
        _hasBackgroundLocationPermission.value =
            (source()?.hasBackgroundLocationPermission() == true)
        if (_wifiIdentityAccessState.value == WifiIdentityAccessState.AVAILABLE) {
            refreshCurrentWifiNetwork()
        } else {
            _currentWifiNetwork.value = null
        }
    }

    private fun refreshCurrentWifiNetwork() {
        val identity = source()?.current()
        _currentWifiNetwork.value = identity
        identity?.let(prefsManager::recordKnownWifiNetwork)
    }
}
