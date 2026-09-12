package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainViewModel(private val prefsManager: PreferencesManager) : ViewModel() {

    val dnsHostnames = prefsManager.dnsHostnames

    // Settings/backup bridge until #121: the app supplies the independent automation owner.
    var onAutomationBackupRestored: (Context) -> Unit = {}

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

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage = _backupStatusMessage.asStateFlow()

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
                onAutomationBackupRestored(appContext)
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

}
