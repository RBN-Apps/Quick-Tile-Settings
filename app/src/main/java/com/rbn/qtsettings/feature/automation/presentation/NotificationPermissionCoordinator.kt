package com.rbn.qtsettings.feature.automation.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NotificationPermissionCoordinator(
    private val preferences: AutomationSettingsSource,
    private val manageVpnMonitoring: () -> Unit,
    private val manageNetworkMonitoring: (restartService: Boolean) -> Unit,
    private val notifyWifiRulesRequireBackgroundDetection: () -> Unit
) {
    private val _requestPermission = MutableStateFlow(0)
    val requestPermission = _requestPermission.asStateFlow()

    private val _showExplanationDialog = MutableStateFlow(false)
    val showExplanationDialog = _showExplanationDialog.asStateFlow()

    private val _showFallbackDialog = MutableStateFlow(false)
    val showFallbackDialog = _showFallbackDialog.asStateFlow()

    private val _showPermissionSettingsDialog = MutableStateFlow(false)
    val showPermissionSettingsDialog = _showPermissionSettingsDialog.asStateFlow()

    private val _explanationFromBackup = MutableStateFlow(false)
    val explanationFromBackup = _explanationFromBackup.asStateFlow()

    private var awaitingSettingsResult = false

    fun setVpnDetectionMode(mode: String) {
        preferences.setVpnDetectionMode(mode)
        manageVpnMonitoring()
    }

    fun setNetworkTypeDetectionMode(mode: String) {
        preferences.setNetworkTypeDetectionMode(mode)
        manageNetworkMonitoring(false)
    }

    fun setNetworkTypeDetectionEnabled(
        enabled: Boolean,
        wifiNetworkRulesEnabled: Boolean
    ) {
        if (enabled &&
            wifiNetworkRulesEnabled &&
            preferences.getNetworkTypeDetectionMode() != BACKGROUND_DETECTION
        ) {
            preferences.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
            notifyWifiRulesRequireBackgroundDetection()
        }

        preferences.setNetworkTypeDetectionEnabled(enabled)
        manageNetworkMonitoring(false)
    }

    fun setWifiNetworkRulesEnabled(enabled: Boolean) {
        val previouslyEnabled = preferences.areWifiNetworkRulesEnabled()
        if (enabled &&
            preferences.isNetworkTypeDetectionEnabled() &&
            preferences.getNetworkTypeDetectionMode() != BACKGROUND_DETECTION
        ) {
            preferences.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
            notifyWifiRulesRequireBackgroundDetection()
        }

        preferences.setWifiNetworkRulesEnabled(enabled)
        val locationForegroundTypeChanged =
            previouslyEnabled != preferences.areWifiNetworkRulesEnabled()
        manageNetworkMonitoring(locationForegroundTypeChanged)
    }

    fun shouldOfferPermissionForBackgroundStatus(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context) &&
            isAnyBackgroundDetectionEnabled()

    fun showMissingPermission(fromBackup: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _explanationFromBackup.value = fromBackup
            _showExplanationDialog.value = true
        }
    }

    fun clearPermissionRequest() {
        _requestPermission.value = 0
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

    fun continueWithoutNotifications() {
        _showExplanationDialog.value = false
        _showFallbackDialog.value = false
        _showPermissionSettingsDialog.value = false
        awaitingSettingsResult = false
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
        _showExplanationDialog.value = false
        if (granted) {
            _showFallbackDialog.value = false
            _showPermissionSettingsDialog.value = false
            awaitingSettingsResult = false
        } else {
            _showFallbackDialog.value = true
        }
        manageVpnMonitoring()
        manageNetworkMonitoring(false)
    }

    fun refreshPermissionAfterSettings(context: Context) {
        if (!awaitingSettingsResult) return

        if (hasNotificationPermission(context.applicationContext)) {
            _showPermissionSettingsDialog.value = false
            awaitingSettingsResult = false
            manageVpnMonitoring()
            manageNetworkMonitoring(false)
        } else {
            _showPermissionSettingsDialog.value = true
        }
    }

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

}
