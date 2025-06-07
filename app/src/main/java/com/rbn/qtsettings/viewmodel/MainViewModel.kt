package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.PermissionUtils
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
    val dnsEnableAutoRevert = prefsManager.dnsEnableAutoRevert
    val dnsAutoRevertDelaySeconds = prefsManager.dnsAutoRevertDelaySeconds


    val usbToggleEnable = prefsManager.usbToggleEnable
    val usbToggleDisable = prefsManager.usbToggleDisable
    val usbEnableAutoRevert = prefsManager.usbEnableAutoRevert
    val usbAutoRevertDelaySeconds = prefsManager.usbAutoRevertDelaySeconds

    val vpnDetectionEnabled = prefsManager.vpnDetectionEnabled
    val vpnDetectionMode = prefsManager.vpnDetectionMode

    val helpShown = prefsManager.helpShown

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

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable = _isShizukuAvailable.asStateFlow()

    private val _appHasShizukuPermission = MutableStateFlow(false)
    val appHasShizukuPermission = _appHasShizukuPermission.asStateFlow()

    private val _isDeviceRooted = MutableStateFlow(false)
    val isDeviceRooted = _isDeviceRooted.asStateFlow()

    private val _permissionGrantStatus = MutableStateFlow<String?>(null)
    val permissionGrantStatus = _permissionGrantStatus.asStateFlow()

    private val _requestNotificationPermission = MutableStateFlow(0)
    val requestNotificationPermission = _requestNotificationPermission.asStateFlow()

    private val _showNotificationSettingsDialog = MutableStateFlow(false)
    val showNotificationSettingsDialog = _showNotificationSettingsDialog.asStateFlow()


    fun setDnsToggleOff(enabled: Boolean) = prefsManager.setDnsToggleOff(enabled)
    fun setDnsToggleAuto(enabled: Boolean) = prefsManager.setDnsToggleAuto(enabled)
    fun setDnsEnableAutoRevert(enabled: Boolean) = prefsManager.setDnsEnableAutoRevert(enabled)
    fun setDnsAutoRevertDelaySeconds(delay: Int) = prefsManager.setDnsAutoRevertDelaySeconds(delay)


    fun setUsbToggleEnable(enabled: Boolean) = prefsManager.setUsbToggleEnable(enabled)
    fun setUsbToggleDisable(enabled: Boolean) = prefsManager.setUsbToggleDisable(enabled)
    fun setUsbEnableAutoRevert(enabled: Boolean) = prefsManager.setUsbEnableAutoRevert(enabled)
    fun setUsbAutoRevertDelaySeconds(delay: Int) = prefsManager.setUsbAutoRevertDelaySeconds(delay)

    fun setVpnDetectionEnabled(enabled: Boolean) {
        prefsManager.setVpnDetectionEnabled(enabled)
        manageVpnMonitoringService()
    }

    fun setVpnDetectionMode(mode: String) {
        if (mode == BACKGROUND_DETECTION) {
            val context = getCurrentContext()
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotificationPermission =
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                if (!hasNotificationPermission) {
                    handleMissingNotificationPermission()
                    return
                }
            }
        }

        prefsManager.setVpnDetectionMode(mode)
        manageVpnMonitoringService()
    }

    private fun handleMissingNotificationPermission() {
        val context = getCurrentContext()
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _requestNotificationPermission.value = _requestNotificationPermission.value + 1
        }
    }

    private fun manageVpnMonitoringService() {
        viewModelScope.launch {
            val context = getCurrentContext() ?: return@launch

            val enabled = prefsManager.isVpnDetectionEnabled()
            val mode = prefsManager.getVpnDetectionMode()

            if (enabled && mode == BACKGROUND_DETECTION) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission =
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission) {
                        _requestNotificationPermission.value =
                            _requestNotificationPermission.value + 1
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

    fun clearNotificationPermissionRequest() {
        _requestNotificationPermission.value = 0
    }

    fun clearNotificationSettingsDialog() {
        _showNotificationSettingsDialog.value = false
    }

    fun onNotificationPermissionPermanentlyDenied() {
        prefsManager.setVpnDetectionMode(TILE_ONLY_DETECTION)
        _showNotificationSettingsDialog.value = true
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        clearNotificationPermissionRequest()
        if (granted) {
            manageVpnMonitoringService()
        } else {
            prefsManager.setVpnDetectionMode(TILE_ONLY_DETECTION)
            _permissionGrantStatus.value =
                "Notification permission denied. Switched to tile-only VPN detection mode."
        }
    }

    fun initializeVpnMonitoring() {
        manageVpnMonitoringService()
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