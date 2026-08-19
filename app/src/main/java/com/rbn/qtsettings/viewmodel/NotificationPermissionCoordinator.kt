package com.rbn.qtsettings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NotificationPermissionCoordinator(
    private val preferences: PreferencesManager,
    private val manageVpnMonitoring: () -> Unit,
    private val manageNetworkMonitoring: () -> Unit
) {
    private val _requestPermission = MutableStateFlow(0)
    val requestPermission = _requestPermission.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    private val _showExplanationDialog = MutableStateFlow(false)
    val showExplanationDialog = _showExplanationDialog.asStateFlow()

    private val _showFallbackDialog = MutableStateFlow(false)
    val showFallbackDialog = _showFallbackDialog.asStateFlow()

    private val _showPermissionSettingsDialog = MutableStateFlow(false)
    val showPermissionSettingsDialog = _showPermissionSettingsDialog.asStateFlow()

    private val _explanationFromBackup = MutableStateFlow(false)
    val explanationFromBackup = _explanationFromBackup.asStateFlow()

    private var pendingVpnDetectionMode: String? = null
    private var pendingNetworkTypeDetectionMode: String? = null
    private var awaitingSettingsResult = false

    fun setVpnDetectionMode(mode: String, context: Context?) {
        if (requiresPermission(mode, context)) {
            pendingVpnDetectionMode = mode
            showMissingPermission(fromBackup = false)
            return
        }
        preferences.setVpnDetectionMode(mode)
        manageVpnMonitoring()
    }

    fun setNetworkTypeDetectionMode(mode: String, context: Context?) {
        if (requiresPermission(mode, context)) {
            pendingNetworkTypeDetectionMode = mode
            showMissingPermission(fromBackup = false)
            return
        }
        preferences.setNetworkTypeDetectionMode(mode)
        manageNetworkMonitoring()
    }

    fun needsPermissionForBackgroundDetection(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context) &&
            isAnyBackgroundDetectionEnabled()

    fun showMissingPermission(fromBackup: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _explanationFromBackup.value = fromBackup
            _showExplanationDialog.value = true
        }
    }

    fun requestPermissionForServiceStart() {
        _requestPermission.value += 1
    }

    fun clearPermissionRequest() {
        _requestPermission.value = 0
    }

    fun clearSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun requestPermissionFromExplanation() {
        _showExplanationDialog.value = false
        _showFallbackDialog.value = false
        _showPermissionSettingsDialog.value = false
        awaitingSettingsResult = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _requestPermission.value += 1
        }
    }

    fun useTileOnlyFallback() {
        _showExplanationDialog.value = false
        _showFallbackDialog.value = false
        _showPermissionSettingsDialog.value = false
        pendingVpnDetectionMode = null
        pendingNetworkTypeDetectionMode = null
        awaitingSettingsResult = false
        fallbackBackgroundDetectionToTileOnly()
    }

    fun openPermissionSettings() {
        clearPermissionRequest()
        _showFallbackDialog.value = false
        _showPermissionSettingsDialog.value = false
        awaitingSettingsResult = true
    }

    fun onPermissionPermanentlyDenied() {
        clearPermissionRequest()
        _showFallbackDialog.value = false
        _showPermissionSettingsDialog.value = true
    }

    fun onPermissionResult(granted: Boolean) {
        clearPermissionRequest()
        if (granted) {
            _showFallbackDialog.value = false
            _showPermissionSettingsDialog.value = false
            awaitingSettingsResult = false
            applyPendingChanges()
            manageVpnMonitoring()
            manageNetworkMonitoring()
        } else {
            _showFallbackDialog.value = true
        }
    }

    fun refreshPermissionAfterSettings(context: Context) {
        if (!awaitingSettingsResult) return

        if (hasNotificationPermission(context.applicationContext)) {
            _showPermissionSettingsDialog.value = false
            awaitingSettingsResult = false
            applyPendingChanges()
            manageVpnMonitoring()
            manageNetworkMonitoring()
        } else {
            _showPermissionSettingsDialog.value = true
        }
    }

    private fun requiresPermission(mode: String, context: Context?): Boolean =
        mode == BACKGROUND_DETECTION &&
            context != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context)

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun isAnyBackgroundDetectionEnabled(): Boolean =
        (preferences.isVpnDetectionEnabled() &&
            preferences.getVpnDetectionMode() == BACKGROUND_DETECTION) ||
            (preferences.isNetworkTypeDetectionEnabled() &&
                preferences.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION)

    private fun applyPendingChanges() {
        pendingVpnDetectionMode?.let(preferences::setVpnDetectionMode)
        pendingVpnDetectionMode = null
        pendingNetworkTypeDetectionMode?.let(preferences::setNetworkTypeDetectionMode)
        pendingNetworkTypeDetectionMode = null
    }

    private fun fallbackBackgroundDetectionToTileOnly() {
        if (
            preferences.isVpnDetectionEnabled() &&
            preferences.getVpnDetectionMode() == BACKGROUND_DETECTION
        ) {
            preferences.setVpnDetectionMode(TILE_ONLY_DETECTION)
            manageVpnMonitoring()
        }
        if (
            preferences.isNetworkTypeDetectionEnabled() &&
            preferences.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION
        ) {
            preferences.setNetworkTypeDetectionMode(TILE_ONLY_DETECTION)
            manageNetworkMonitoring()
        }
    }
}
