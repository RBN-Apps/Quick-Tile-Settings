package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.SystemQuickActionResult
import com.rbn.qtsettings.utils.SystemQuickActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainViewModel(private val prefsManager: PreferencesManager) : ViewModel() {

    val dnsToggleOff = prefsManager.dnsToggleOff
    val dnsToggleAuto = prefsManager.dnsToggleAuto
    val dnsHostnames = prefsManager.dnsHostnames
    val dnsListSortMode = prefsManager.dnsListSortMode
    val dnsEnableAutoRevert = prefsManager.dnsEnableAutoRevert
    val dnsAutoRevertDelaySeconds = prefsManager.dnsAutoRevertDelaySeconds


    val usbToggleEnable = prefsManager.usbToggleEnable
    val usbToggleDisable = prefsManager.usbToggleDisable
    val usbAlsoHideDevOptions = prefsManager.usbAlsoHideDevOptions
    val usbAlsoDisableWirelessDebugging = prefsManager.usbAlsoDisableWirelessDebugging
    val usbEnableAutoRevert = prefsManager.usbEnableAutoRevert
    val usbAutoRevertDelaySeconds = prefsManager.usbAutoRevertDelaySeconds

    val vpnDetectionEnabled = prefsManager.vpnDetectionEnabled
    val vpnDetectionMode = prefsManager.vpnDetectionMode

    val networkTypeDetectionEnabled = prefsManager.networkTypeDetectionEnabled
    val networkTypeDetectionMode = prefsManager.networkTypeDetectionMode
    val dnsStateOnWifi = prefsManager.dnsStateOnWifi
    val dnsHostnameOnWifi = prefsManager.dnsHostnameOnWifi
    val dnsStateOnMobile = prefsManager.dnsStateOnMobile
    val dnsHostnameOnMobile = prefsManager.dnsHostnameOnMobile

    val dnsRequireUnlock = prefsManager.dnsRequireUnlock
    val usbRequireUnlock = prefsManager.usbRequireUnlock

    val helpShown = prefsManager.helpShown

    val shortcutMaxCount = prefsManager.shortcutMaxCount
    val enabledShortcutIds = prefsManager.enabledShortcutIds
    val favoriteShortcutIds = prefsManager.favoriteShortcutIds
    val allowPinnedShortcutsWhenDisabled = prefsManager.allowPinnedShortcutsWhenDisabled

    private val _initialTab = MutableStateFlow(0)
    val initialTab = _initialTab.asStateFlow()

    private val _showHostnameEditDialog = MutableStateFlow(false)
    val showHostnameEditDialog = _showHostnameEditDialog.asStateFlow()

    private val _editingHostname = MutableStateFlow<DnsHostnameEntry?>(null)
    val editingHostname = _editingHostname.asStateFlow()

    private val _hostnamePendingDeletion = MutableStateFlow<DnsHostnameEntry?>(null)
    val hostnamePendingDeletion = _hostnamePendingDeletion.asStateFlow()

    private val _hasWriteSecureSettings = MutableStateFlow(false)
    val hasWriteSecureSettings = _hasWriteSecureSettings.asStateFlow()

    private val _activeDnsHostname = MutableStateFlow<String?>(null)
    val activeDnsHostname = _activeDnsHostname.asStateFlow()

    private val _activeDnsMode = MutableStateFlow<String?>(null)
    val activeDnsMode = _activeDnsMode.asStateFlow()

    private val _usbDebuggingEnabled = MutableStateFlow(false)
    val usbDebuggingEnabled = _usbDebuggingEnabled.asStateFlow()

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

    private val notificationPermissionCoordinator = NotificationPermissionCoordinator(
        preferences = prefsManager,
        manageVpnMonitoring = ::manageVpnMonitoringService,
        manageNetworkMonitoring = ::manageNetworkMonitoringService
    )
    val requestNotificationPermission = notificationPermissionCoordinator.requestPermission
    val showNotificationSettingsDialog = notificationPermissionCoordinator.showSettingsDialog
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

    private val _quickActionStatusMessage = MutableStateFlow<String?>(null)
    val quickActionStatusMessage = _quickActionStatusMessage.asStateFlow()

    fun setDnsToggleOff(enabled: Boolean) = prefsManager.setDnsToggleOff(enabled)
    fun setDnsToggleAuto(enabled: Boolean) = prefsManager.setDnsToggleAuto(enabled)
    fun setDnsListSortMode(mode: DnsListSortMode) = prefsManager.setDnsListSortMode(mode)
    fun reorderDnsHostnames(orderedIds: List<String>): Boolean =
        prefsManager.reorderDnsHostnames(orderedIds)

    fun setDnsEnableAutoRevert(enabled: Boolean) = prefsManager.setDnsEnableAutoRevert(enabled)
    fun setDnsAutoRevertDelaySeconds(delay: Int) = prefsManager.setDnsAutoRevertDelaySeconds(delay)


    fun setUsbToggleEnable(enabled: Boolean) = prefsManager.setUsbToggleEnable(enabled)
    fun setUsbToggleDisable(enabled: Boolean) = prefsManager.setUsbToggleDisable(enabled)
    fun setUsbAlsoHideDevOptions(enabled: Boolean) = prefsManager.setUsbAlsoHideDevOptions(enabled)
    fun setUsbAlsoDisableWirelessDebugging(enabled: Boolean) =
        prefsManager.setUsbAlsoDisableWirelessDebugging(enabled)

    fun setUsbEnableAutoRevert(enabled: Boolean) = prefsManager.setUsbEnableAutoRevert(enabled)
    fun setUsbAutoRevertDelaySeconds(delay: Int) = prefsManager.setUsbAutoRevertDelaySeconds(delay)

    fun setDnsRequireUnlock(enabled: Boolean) = prefsManager.setDnsRequireUnlock(enabled)
    fun setUsbRequireUnlock(enabled: Boolean) = prefsManager.setUsbRequireUnlock(enabled)

    fun setVpnDetectionEnabled(enabled: Boolean) {
        prefsManager.setVpnDetectionEnabled(enabled)
        manageVpnMonitoringService()
    }

    fun setVpnDetectionMode(mode: String) {
        notificationPermissionCoordinator.setVpnDetectionMode(mode, getCurrentContext())
    }

    fun setNetworkTypeDetectionEnabled(enabled: Boolean) {
        prefsManager.setNetworkTypeDetectionEnabled(enabled)
        manageNetworkMonitoringService()
    }

    fun setNetworkTypeDetectionMode(mode: String) {
        notificationPermissionCoordinator.setNetworkTypeDetectionMode(mode, getCurrentContext())
    }

    fun setDnsStateOnWifi(state: String) = prefsManager.setDnsStateOnWifi(state)
    fun setDnsHostnameOnWifi(hostname: String?) = prefsManager.setDnsHostnameOnWifi(hostname)
    fun setDnsStateOnMobile(state: String) = prefsManager.setDnsStateOnMobile(state)
    fun setDnsHostnameOnMobile(hostname: String?) = prefsManager.setDnsHostnameOnMobile(hostname)
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
                if (notificationPermissionCoordinator.needsPermissionForBackgroundDetection(appContext)) {
                    notificationPermissionCoordinator.showMissingPermission(fromBackup = true)
                } else {
                    manageVpnMonitoringService()
                    manageNetworkMonitoringService()
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

            if (enabled && mode == BACKGROUND_DETECTION) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission) {
                        notificationPermissionCoordinator.requestPermissionForServiceStart()
                        return@launch
                    }
                }

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

    fun setInitialTab(tabIndex: Int) {
        _initialTab.value = tabIndex
    }

    fun updateDnsHostnameEntrySelection(id: String, isSelected: Boolean) {
        val entry = dnsHostnames.value.find { it.id == id }
        entry?.let {
            prefsManager.updateDnsHostnameEntry(it.copy(isSelectedForCycle = isSelected))
        }
    }

    fun addCustomDnsHostname(name: String, hostnameValue: String) {
        prefsManager.addCustomDnsHostname(name, hostnameValue)
    }

    fun editCustomDnsHostname(id: String, newName: String, newHostnameValue: String) {
        val entry = dnsHostnames.value.find { it.id == id && !it.isPredefined }
        entry?.let {
            prefsManager.updateDnsHostnameEntry(
                it.copy(
                    name = newName,
                    hostname = newHostnameValue
                )
            )
        }
    }

    fun deleteCustomDnsHostname(id: String) {
        prefsManager.deleteCustomDnsHostname(id)
    }

    fun startAddingNewHostname() {
        _editingHostname.value = null; _showHostnameEditDialog.value = true
    }

    fun startEditingHostname(entry: DnsHostnameEntry) {
        _editingHostname.value = entry; _showHostnameEditDialog.value = true
    }

    fun dismissHostnameEditDialog() {
        _showHostnameEditDialog.value = false; _editingHostname.value = null
    }

    fun setHostnamePendingDeletion(entry: DnsHostnameEntry?) {
        _hostnamePendingDeletion.value = entry
    }

    fun setActiveDns(context: Context, entry: DnsHostnameEntry) {
        val appContext = context.applicationContext
        val result = SystemQuickActions.setActiveDns(appContext, entry.hostname)
        _quickActionStatusMessage.value = when (result) {
            SystemQuickActionResult.SUCCESS ->
                appContext.getString(R.string.shortcut_toast_dns_hostname, entry.name)

            SystemQuickActionResult.PERMISSION_MISSING ->
                appContext.getString(R.string.toast_permission_not_granted_adb)

            SystemQuickActionResult.INVALID_DNS_HOSTNAME ->
                appContext.getString(R.string.error_hostname_value_invalid)

            SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED,
            SystemQuickActionResult.FAILED ->
                appContext.getString(R.string.toast_error_saving_settings)
        }
        refreshSystemQuickActionStates(appContext)
    }

    fun setDnsMode(context: Context, mode: String) {
        val appContext = context.applicationContext
        val result = SystemQuickActions.setDnsMode(appContext, mode)
        _quickActionStatusMessage.value = when (result) {
            SystemQuickActionResult.SUCCESS -> appContext.getString(
                if (mode == DNS_MODE_OFF) {
                    R.string.shortcut_toast_dns_off
                } else {
                    R.string.shortcut_toast_dns_auto
                }
            )

            SystemQuickActionResult.PERMISSION_MISSING ->
                appContext.getString(R.string.toast_permission_not_granted_adb)

            SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED,
            SystemQuickActionResult.INVALID_DNS_HOSTNAME,
            SystemQuickActionResult.FAILED ->
                appContext.getString(R.string.toast_error_saving_settings)
        }
        refreshSystemQuickActionStates(appContext)
    }

    fun setUsbDebuggingEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val result = SystemQuickActions.setUsbDebuggingEnabled(
            context = appContext,
            enabled = enabled,
            toggleDeveloperOptions = prefsManager.isUsbAlsoHideDevOptionsEnabled(),
            toggleWirelessDebugging = prefsManager.isUsbAlsoDisableWirelessDebuggingEnabled()
        )
        _quickActionStatusMessage.value = when (result) {
            SystemQuickActionResult.SUCCESS -> appContext.getString(
                if (enabled) R.string.shortcut_toast_usb_on else R.string.shortcut_toast_usb_off
            )

            SystemQuickActionResult.PERMISSION_MISSING ->
                appContext.getString(R.string.toast_permission_not_granted_adb)

            SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED ->
                appContext.getString(R.string.toast_developer_options_disabled)

            SystemQuickActionResult.INVALID_DNS_HOSTNAME,
            SystemQuickActionResult.FAILED ->
                appContext.getString(R.string.toast_error_saving_settings)
        }
        refreshSystemQuickActionStates(appContext)
    }

    fun clearQuickActionStatusMessage() {
        _quickActionStatusMessage.value = null
    }

    fun refreshSystemQuickActionStates(context: Context) {
        _activeDnsHostname.value = SystemQuickActions.getActiveDnsHostname(context)
        _activeDnsMode.value = SystemQuickActions.getActiveDnsMode(context)
        _usbDebuggingEnabled.value = SystemQuickActions.isUsbDebuggingEnabled(context)
        _developerOptionsEnabled.value = PermissionUtils.isDeveloperOptionsEnabled(context)
    }

    fun checkSystemStates(context: Context) {
        _hasWriteSecureSettings.value = PermissionUtils.hasWriteSecureSettingsPermission(context)
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

    fun clearNotificationSettingsDialog() {
        notificationPermissionCoordinator.clearSettingsDialog()
    }

    fun requestNotificationPermissionFromExplanation() {
        notificationPermissionCoordinator.requestPermissionFromExplanation()
    }

    fun useTileOnlyDetectionForNotificationFallback() {
        notificationPermissionCoordinator.useTileOnlyFallback()
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

    private fun manageNetworkMonitoringService() {
        viewModelScope.launch {
            val context = getCurrentContext() ?: return@launch

            val enabled = prefsManager.isNetworkTypeDetectionEnabled()
            val mode = prefsManager.getNetworkTypeDetectionMode()

            if (enabled && mode == BACKGROUND_DETECTION) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission) {
                        notificationPermissionCoordinator.requestPermissionForServiceStart()
                        return@launch
                    }
                }

                NetworkMonitoringService.startService(context)
            } else {
                NetworkMonitoringService.stopService(context)
            }
        }
    }

    fun initializeNetworkMonitoring() {
        manageNetworkMonitoringService()
    }
}

class ViewModelFactory(private val prefsManager: PreferencesManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
