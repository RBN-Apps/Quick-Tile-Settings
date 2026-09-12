package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.WifiNetworkDiscovery
import com.rbn.qtsettings.utils.WifiNetworkRuleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainViewModel(private val prefsManager: PreferencesManager) : ViewModel() {

    val dnsHostnames = prefsManager.dnsHostnames

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

    val helpShown = prefsManager.helpShown

    val shortcutMaxCount = prefsManager.shortcutMaxCount
    val enabledShortcutIds = prefsManager.enabledShortcutIds
    val favoriteShortcutIds = prefsManager.favoriteShortcutIds
    val allowPinnedShortcutsWhenDisabled = prefsManager.allowPinnedShortcutsWhenDisabled

    private val _hasWriteSecureSettings = MutableStateFlow(false)
    val hasWriteSecureSettings = _hasWriteSecureSettings.asStateFlow()

    private val _developerOptionsEnabled = MutableStateFlow(false)
    val developerOptionsEnabled = _developerOptionsEnabled.asStateFlow()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable = _isShizukuAvailable.asStateFlow()

    private val _appHasShizukuPermission = MutableStateFlow(false)
    val appHasShizukuPermission = _appHasShizukuPermission.asStateFlow()

    private val _isDeviceRooted = MutableStateFlow(false)
    val isDeviceRooted = _isDeviceRooted.asStateFlow()

    private val _permissionGrantStatus = MutableStateFlow<String?>(null)
    val permissionGrantStatus = _permissionGrantStatus.asStateFlow()

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

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage = _backupStatusMessage.asStateFlow()

    private val _networkDetectionStatusMessage = MutableStateFlow<String?>(null)
    val networkDetectionStatusMessage = _networkDetectionStatusMessage.asStateFlow()

    fun setVpnDetectionEnabled(enabled: Boolean) {
        prefsManager.setVpnDetectionEnabled(enabled)
        manageVpnMonitoringService()
    }

    fun setVpnDetectionMode(mode: String) {
        notificationPermissionCoordinator.setVpnDetectionMode(mode, getCurrentContext())
    }

    fun setNetworkTypeDetectionEnabled(enabled: Boolean) {
        notificationPermissionCoordinator.setNetworkTypeDetectionEnabled(
            enabled = enabled,
            wifiNetworkRulesEnabled = wifiNetworkRulesEnabled(),
            context = getCurrentContext()
        )
    }

    fun setNetworkTypeDetectionMode(mode: String) {
        if (mode == TILE_ONLY_DETECTION && wifiNetworkRulesEnabled()) {
            getCurrentContext()?.let { context ->
                _permissionGrantStatus.value =
                    context.getString(R.string.wifi_rules_require_background_detection)
            }
            return
        }

        notificationPermissionCoordinator.setNetworkTypeDetectionMode(mode, getCurrentContext())
    }

    fun setDnsStateOnWifi(state: String) {
        prefsManager.setDnsStateOnWifi(state)
        reevaluateNetworkAutomation()
    }

    fun setDnsHostnameOnWifi(hostname: String?) {
        prefsManager.setDnsHostnameOnWifi(hostname)
        reevaluateNetworkAutomation()
    }

    fun setWifiNetworkRulesEnabled(enabled: Boolean) {
        notificationPermissionCoordinator.setWifiNetworkRulesEnabled(enabled, getCurrentContext())
    }

    fun addWifiNetworkRule(
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

    fun updateWifiNetworkRule(
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

    fun deleteWifiNetworkRule(ruleId: String) {
        prefsManager.deleteWifiNetworkRule(ruleId)
        reevaluateNetworkAutomation()
    }

    fun refreshWifiNetworks() {
        if (_isWifiResultsLoading.value) return
        val context = getCurrentContext() ?: return
        if (!PermissionUtils.canAccessWifiSsid(context)) {
            launchWifiSsidPermissionRequest()
            return
        }

        viewModelScope.launch {
            _isWifiResultsLoading.value = true
            try {
                refreshCurrentWifiNetwork(context)
                _scannedWifiNetworks.value = WifiNetworkDiscovery.loadLatestResults(context)
            } finally {
                _isWifiResultsLoading.value = false
            }
        }
    }

    fun launchWifiSsidPermissionRequest() {
        val context = getCurrentContext()
        if (context != null && PermissionUtils.hasPreciseLocationPermission(context)) {
            _wifiIdentityAccessState.value = PermissionUtils.getWifiIdentityAccessState(context)
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
            PermissionUtils.getWifiIdentityAccessState(context)
        } else {
            WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
        }
        if (_wifiIdentityAccessState.value == WifiIdentityAccessState.AVAILABLE) {
            reevaluateNetworkAutomation()
            refreshWifiNetworks()
        } else if (context != null) {
            _permissionGrantStatus.value = context.getString(
                if (granted && PermissionUtils.hasPreciseLocationPermission(context)) {
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

    fun setDnsStateOnMobile(state: String) {
        prefsManager.setDnsStateOnMobile(state)
        reevaluateNetworkAutomation()
    }

    fun setDnsHostnameOnMobile(hostname: String?) {
        prefsManager.setDnsHostnameOnMobile(hostname)
        reevaluateNetworkAutomation()
    }
    fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean): Boolean =
        prefsManager.setShortcutExposureEnabled(shortcutId, enabled)
    fun setShortcutFavorite(shortcutId: String, favorite: Boolean): Boolean =
        prefsManager.setShortcutFavorite(shortcutId, favorite)
    fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) =
        prefsManager.setAllowPinnedShortcutsWhenDisabled(enabled)
    fun refreshShortcutConfiguration() = prefsManager.refreshShortcutConfiguration()

    fun exportBackup(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.writer(Charsets.UTF_8).use { writer ->
                        writer.write(prefsManager.exportSettingsBackupJson())
                    }
                } ?: throw IllegalStateException("Could not open backup destination")

                _backupStatusMessage.value = appContext.getString(R.string.backup_export_success)
            } catch (e: Exception) {
                Log.e("SettingsBackup", "Backup export failed", e)
                _backupStatusMessage.value =
                    appContext.getString(R.string.backup_export_error, e.message ?: "Unknown error")
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } ?: throw IllegalStateException("Could not open backup file")

                prefsManager.restoreSettingsBackupJson(json)
                enforceBackgroundDetectionForWifiRules()
                manageVpnMonitoringService()
                manageNetworkMonitoringService(restartService = true)
                if (notificationPermissionCoordinator.shouldOfferPermissionForBackgroundStatus(
                        appContext
                    )
                ) {
                    notificationPermissionCoordinator.showMissingPermission(fromBackup = true)
                }
                _backupStatusMessage.value = appContext.getString(R.string.backup_restore_success)
            } catch (e: Exception) {
                Log.e("SettingsBackup", "Backup restore failed", e)
                _backupStatusMessage.value =
                    appContext.getString(R.string.backup_restore_error, e.message ?: "Unknown error")
            }
        }
    }

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
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

    private var applicationContext: Context? = null

    fun setApplicationContext(context: Context) {
        applicationContext = context.applicationContext
    }

    private fun getCurrentContext(): Context? = applicationContext

    fun setHelpShown(shown: Boolean) = prefsManager.setHelpShown(shown)

    fun refreshSystemQuickActionStates(context: Context) {
        _developerOptionsEnabled.value = PermissionUtils.isDeveloperOptionsEnabled(context)
    }

    fun checkSystemStates(context: Context) {
        _hasWriteSecureSettings.value = PermissionUtils.hasWriteSecureSettingsPermission(context)
        _wifiIdentityAccessState.value = PermissionUtils.getWifiIdentityAccessState(context)
        _hasBackgroundLocationPermission.value =
            PermissionUtils.hasBackgroundLocationPermission(context)
        if (_wifiIdentityAccessState.value == WifiIdentityAccessState.AVAILABLE) {
            refreshCurrentWifiNetwork(context.applicationContext)
        } else {
            _currentWifiNetwork.value = null
        }
        _isShizukuAvailable.value = PermissionUtils.isShizukuAvailableAndReady()
        if (_isShizukuAvailable.value) {
            _appHasShizukuPermission.value =
                PermissionUtils.checkShizukuPermission(context) == PackageManager.PERMISSION_GRANTED
        } else {
            _appHasShizukuPermission.value = false
        }
        _isDeviceRooted.value = PermissionUtils.isDeviceRooted()
        refreshSystemQuickActionStates(context.applicationContext)
    }

    fun grantWriteSecureSettingsViaShizuku(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_isShizukuAvailable.value) {
                _permissionGrantStatus.value = context.getString(R.string.shizuku_not_available)
                return@launch
            }
            if (!_appHasShizukuPermission.value) {
                _permissionGrantStatus.value =
                    context.getString(R.string.shizuku_permission_not_granted_to_app_prompt)
                return@launch
            }

            try {
                val packageName = context.packageName
                val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)

                val deferredStdErr =
                    async { process.errorStream.bufferedReader().use { it.readText() } }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    // Re-check permission directly
                    if (PermissionUtils.hasWriteSecureSettingsPermission(context.applicationContext)) {
                        _permissionGrantStatus.value =
                            context.getString(R.string.permission_granted_shizuku_success)
                    } else {
                        _permissionGrantStatus.value =
                            context.getString(R.string.permission_granted_shizuku_check_failed)
                    }
                } else {
                    val errOutput = deferredStdErr.await()
                    Log.e(
                        "ShizukuGrant",
                        "Shizuku command failed with exit code $exitCode: $errOutput"
                    )
                    _permissionGrantStatus.value =
                        context.getString(R.string.permission_granted_shizuku_fail, exitCode)
                }
            } catch (e: Exception) {
                Log.e("ShizukuGrant", "Error granting permission via Shizuku", e)
                _permissionGrantStatus.value =
                    context.getString(R.string.permission_granted_shizuku_error, e.message)
            } finally {
                checkSystemStates(context.applicationContext)
            }
        }
    }

    fun grantWriteSecureSettingsViaRoot(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_isDeviceRooted.value) {
                _permissionGrantStatus.value = context.getString(R.string.device_not_rooted)
                return@launch
            }
            try {
                val packageName = context.packageName
                val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

                val deferredStdErr =
                    async { process.errorStream.bufferedReader().use { it.readText() } }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    if (PermissionUtils.hasWriteSecureSettingsPermission(context.applicationContext)) {
                        _permissionGrantStatus.value =
                            context.getString(R.string.permission_granted_root_success)
                    } else {
                        _permissionGrantStatus.value =
                            context.getString(R.string.permission_granted_root_check_failed)
                    }
                } else {
                    val errOutput = deferredStdErr.await()
                    Log.e("RootGrant", "Root command failed with exit code $exitCode: $errOutput")
                    _permissionGrantStatus.value =
                        context.getString(R.string.permission_granted_root_fail, exitCode)
                }
            } catch (e: Exception) {
                Log.e("RootGrant", "Error granting permission via Root", e)
                _permissionGrantStatus.value =
                    context.getString(R.string.permission_granted_root_error, e.message)
            } finally {
                checkSystemStates(context.applicationContext)
            }
        }
    }

    fun clearPermissionGrantStatus() {
        _permissionGrantStatus.value = null
    }

    fun clearNetworkDetectionStatusMessage() {
        _networkDetectionStatusMessage.value = null
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

    private fun refreshCurrentWifiNetwork(context: Context) {
        val state = NetworkTypeDetectionUtils.getCurrentNetworkState(
            context = context,
            includeWifiSsid = true
        )
        val identity = if (state.networkType == NETWORK_TYPE_WIFI) {
            WifiNetworkRuleUtils.normalizeIdentity(
                WifiNetworkIdentity(ssid = state.wifiSsid, bssid = state.wifiBssid)
            )
        } else {
            null
        }
        _currentWifiNetwork.value = identity
        identity?.let(prefsManager::recordKnownWifiNetwork)
    }
}
