package com.rbn.qtsettings.feature.automation.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.feature.automation.domain.WifiDiscoverySource
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AutomationViewModel(
    private val prefsManager: AutomationSettingsSource,
    private val discoveryFactory: (Context) -> WifiDiscoverySource
) : ViewModel(), AutomationUiActions {
    val vpnDetectionEnabled = prefsManager.vpnDetectionEnabled
    val vpnDetectionMode = prefsManager.vpnDetectionMode

    val networkTypeDetectionEnabled = prefsManager.networkTypeDetectionEnabled
    val networkTypeDetectionMode = prefsManager.networkTypeDetectionMode
    val dnsStateOnWifi = prefsManager.dnsStateOnWifi
    val dnsHostnameOnWifi = prefsManager.dnsHostnameOnWifi
    val wifiNetworkRulesEnabled = prefsManager.wifiNetworkRulesEnabled
    val wifiNetworkRules = prefsManager.wifiNetworkRules
    val knownWifiNetworks = prefsManager.knownWifiNetworks
    val dnsStateOnMobile = prefsManager.dnsStateOnMobile
    val dnsHostnameOnMobile = prefsManager.dnsHostnameOnMobile

    private var discovery: WifiDiscoverySource? = null
    private val notificationPermissionCoordinator = NotificationPermissionCoordinator(
        preferences = prefsManager,
        manageVpnMonitoring = ::manageVpnMonitoringService,
        manageNetworkMonitoring = ::manageNetworkMonitoringService,
        notifyWifiRulesRequireBackgroundDetection =
            ::notifyWifiRulesRequireBackgroundDetection
    )
    val requestNotificationPermission = notificationPermissionCoordinator.requestPermission
    val showNotificationPermissionExplanationDialog =
        notificationPermissionCoordinator.showExplanationDialog
    val showNotificationPermissionFallbackDialog =
        notificationPermissionCoordinator.showFallbackDialog
    val showNotificationPermissionSettingsDialog =
        notificationPermissionCoordinator.showPermissionSettingsDialog
    val notificationPermissionExplanationFromBackup =
        notificationPermissionCoordinator.explanationFromBackup

    private val _networkDetectionStatusMessage = MutableStateFlow<String?>(null)
    val networkDetectionStatusMessage = _networkDetectionStatusMessage.asStateFlow()

    private val _permissionGrantStatus = MutableStateFlow<String?>(null)
    val permissionGrantStatus = _permissionGrantStatus.asStateFlow()
    private val wifi by lazy {
        WifiDiscoveryController(
            source = { discovery },
            getCurrentContext = ::getCurrentContext,
            prefsManager = prefsManager,
            viewModelScope = viewModelScope,
            _permissionGrantStatus = _permissionGrantStatus,
            reevaluateNetworkAutomation = { reevaluateNetworkAutomation() }
        )
    }
    val requestWifiSsidPermission get() = wifi.requestWifiSsidPermission
    val wifiIdentityAccessState get() = wifi.wifiIdentityAccessState
    val hasBackgroundLocationPermission get() = wifi.hasBackgroundLocationPermission
    val currentWifiNetwork get() = wifi.currentWifiNetwork
    val scannedWifiNetworks get() = wifi.scannedWifiNetworks
    val isWifiResultsLoading get() = wifi.isWifiResultsLoading
    override fun refreshWifiNetworks() = wifi.refreshWifiNetworks()
    override fun launchWifiSsidPermissionRequest() = wifi.launchWifiSsidPermissionRequest()
    fun onWifiSsidPermissionResult(granted: Boolean) = wifi.onWifiSsidPermissionResult(granted)
    fun clearWifiSsidPermissionRequest() = wifi.clearWifiSsidPermissionRequest()

    private val _notificationsAllowed = MutableStateFlow(true)
    val notificationsAllowed = _notificationsAllowed.asStateFlow()

    private var applicationContext: Context? = null
    fun setApplicationContext(context: Context) {
        applicationContext = context.applicationContext
        discovery = discoveryFactory(context.applicationContext)
    }
    private fun getCurrentContext(): Context? = applicationContext
    fun clearPermissionGrantStatus() { _permissionGrantStatus.value = null }
    fun checkSystemStates(context: Context) {
        if (applicationContext == null) setApplicationContext(context)
        wifi.refreshSystemStates()
        _notificationsAllowed.value = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun onBackupRestored(context: Context) {
        enforceBackgroundDetectionForWifiRules()
        manageVpnMonitoringService()
        manageNetworkMonitoringService(restartService = true)
        if (notificationPermissionCoordinator.shouldOfferPermissionForBackgroundStatus(context)) {
            notificationPermissionCoordinator.showMissingPermission(fromBackup = true)
        }
    }

    override fun setVpnDetectionEnabled(enabled: Boolean) {
        prefsManager.setVpnDetectionEnabled(enabled)
        manageVpnMonitoringService()
    }

    override fun setVpnDetectionMode(mode: String) {
        notificationPermissionCoordinator.setVpnDetectionMode(mode)
    }

    override fun setNetworkTypeDetectionEnabled(enabled: Boolean) {
        notificationPermissionCoordinator.setNetworkTypeDetectionEnabled(
            enabled = enabled,
            wifiNetworkRulesEnabled = wifiNetworkRulesEnabled()
        )
    }

    override fun setNetworkTypeDetectionMode(mode: String) {
        if (mode == TILE_ONLY_DETECTION && wifiNetworkRulesEnabled()) {
            getCurrentContext()?.let { context ->
                _permissionGrantStatus.value =
                    context.getString(R.string.wifi_rules_require_background_detection)
            }
            return
        }

        notificationPermissionCoordinator.setNetworkTypeDetectionMode(mode)
    }

    override fun setDnsStateOnWifi(state: String) {
        prefsManager.setDnsStateOnWifi(state)
        reevaluateNetworkAutomation()
    }

    override fun setDnsHostnameOnWifi(hostname: String?) {
        prefsManager.setDnsHostnameOnWifi(hostname)
        reevaluateNetworkAutomation()
    }

    override fun setWifiNetworkRulesEnabled(enabled: Boolean) {
        notificationPermissionCoordinator.setWifiNetworkRulesEnabled(enabled)
    }

    override fun addWifiNetworkRule(
        ssid: String?,
        bssid: String?,
        actionMode: String,
        dnsHostname: String?
    ): Boolean {
        val added = prefsManager.addWifiNetworkRule(
            WifiDnsRule(
                ssid = ssid,
                bssid = bssid,
                actionMode = actionMode,
                dnsHostname = dnsHostname
            )
        )
        if (added) reevaluateNetworkAutomation()
        return added
    }

    override fun updateWifiNetworkRule(
        id: String,
        ssid: String?,
        bssid: String?,
        actionMode: String,
        dnsHostname: String?
    ): Boolean {
        val updated = prefsManager.updateWifiNetworkRule(
            WifiDnsRule(
                id = id,
                ssid = ssid,
                bssid = bssid,
                actionMode = actionMode,
                dnsHostname = dnsHostname
            )
        )
        if (updated) reevaluateNetworkAutomation()
        return updated
    }

    override fun deleteWifiNetworkRule(ruleId: String) {
        prefsManager.deleteWifiNetworkRule(ruleId)
        reevaluateNetworkAutomation()
    }

    override fun setDnsStateOnMobile(state: String) {
        prefsManager.setDnsStateOnMobile(state)
        reevaluateNetworkAutomation()
    }

    override fun setDnsHostnameOnMobile(hostname: String?) {
        prefsManager.setDnsHostnameOnMobile(hostname)
        reevaluateNetworkAutomation()
    }
    private fun manageVpnMonitoringService() {
        viewModelScope.launch {
            val context = getCurrentContext() ?: return@launch

            val enabled = prefsManager.isVpnDetectionEnabled()
            val mode = prefsManager.getVpnDetectionMode()
            val hasWriteSecureSettings =
                PermissionUtils.hasWriteSecureSettingsPermission(context)

            if (enabled && mode == BACKGROUND_DETECTION && hasWriteSecureSettings) {
                VpnMonitoringService.startService(context)
            } else {
                VpnMonitoringService.stopService(context)
            }
        }
    }

    fun clearNetworkDetectionStatusMessage() {
        _networkDetectionStatusMessage.value = null
    }

    fun showOptionalNotificationExplanation() {
        notificationPermissionCoordinator.showMissingPermission(fromBackup = false)
    }

    fun clearNotificationPermissionRequest() {
        notificationPermissionCoordinator.clearPermissionRequest()
    }

    fun requestNotificationPermissionFromExplanation() {
        notificationPermissionCoordinator.requestPermissionFromExplanation()
    }

    fun continueWithoutNotificationPermission() {
        notificationPermissionCoordinator.continueWithoutNotifications()
    }

    fun openNotificationPermissionSettings() {
        notificationPermissionCoordinator.openPermissionSettings()
    }

    fun onNotificationPermissionPermanentlyDenied() {
        notificationPermissionCoordinator.onPermissionPermanentlyDenied()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        notificationPermissionCoordinator.onPermissionResult(granted)
        getCurrentContext()?.let(::checkSystemStates)
    }

    fun refreshNotificationPermissionAfterSettings(context: Context) {
        notificationPermissionCoordinator.refreshPermissionAfterSettings(context)
    }

    fun initializeVpnMonitoring() {
        manageVpnMonitoringService()
    }

    private fun manageNetworkMonitoringService(
        restartService: Boolean = false,
        reapplyPolicy: Boolean = true
    ) {
        viewModelScope.launch {
            val context = getCurrentContext() ?: return@launch

            val enabled = prefsManager.isNetworkTypeDetectionEnabled()
            val mode = prefsManager.getNetworkTypeDetectionMode()
            val hasWriteSecureSettings =
                PermissionUtils.hasWriteSecureSettingsPermission(context)

            if (enabled && mode == BACKGROUND_DETECTION && hasWriteSecureSettings) {
                if (restartService) {
                    NetworkMonitoringService.stopService(context)
                }
                NetworkMonitoringService.startService(context, reapplyPolicy)
            } else {
                NetworkMonitoringService.stopService(context)
            }
        }
    }

    private fun reevaluateNetworkAutomation(restartService: Boolean = false) {
        if (prefsManager.isNetworkTypeDetectionEnabled() &&
            prefsManager.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION
        ) {
            manageNetworkMonitoringService(restartService)
        }
    }

    private fun wifiNetworkRulesEnabled(): Boolean =
        prefsManager.areWifiNetworkRulesEnabled()

    private fun enforceBackgroundDetectionForWifiRules() {
        if (wifiNetworkRulesEnabled()) {
            prefsManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        }
    }

    private fun notifyWifiRulesRequireBackgroundDetection() {
        getCurrentContext()?.let { context ->
            _networkDetectionStatusMessage.value =
                context.getString(R.string.wifi_rules_enabled_background_detection)
        }
    }

    fun initializeNetworkMonitoring() {
        manageNetworkMonitoringService(reapplyPolicy = false)
    }

}
