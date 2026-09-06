package com.rbn.qtsettings.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.rbn.qtsettings.MainActivity
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.NetworkDnsAutomation
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NetworkMonitoringService : Service() {

    companion object {
        private const val TAG = "NetworkMonitoringService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "network_monitoring_channel"
        private const val CHECK_INTERVAL_MS = 3000L // Check every 3 seconds
        private const val EXTRA_REAPPLY_POLICY = "reapply_policy"

        fun startService(context: Context, reapplyPolicy: Boolean = true) {
            val intent = Intent(context, NetworkMonitoringService::class.java).apply {
                putExtra(EXTRA_REAPPLY_POLICY, reapplyPolicy)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetworkMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var prefsManager: PreferencesManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var serviceJob: Job? = null
    private var wifiIdentityFallbackJob: Job? = null
    private var currentNetworkState = DetectedNetworkState()
    private var monitorWifiSsid = false
    private var initialNetworkInventoryPending = false
    private var forceNextResolvedState = false
    private var vpnOverrideWasActive = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
        createNotificationChannel()
        Log.d(TAG, "Network monitoring service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every startForegroundService() call must be paired with startForeground(), even when a
        // changed setting or revoked permission means this service has to stop immediately.
        startForegroundBase()

        if (!prefsManager.isNetworkTypeDetectionEnabled() ||
            prefsManager.getNetworkTypeDetectionMode() != BACKGROUND_DETECTION) {
            Log.d(TAG, "Network type detection disabled or not in background mode, stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Log.w(TAG, "No WRITE_SECURE_SETTINGS permission, stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForegroundForCurrentConfiguration()
        val desiredMonitorWifiSsid = prefsManager.areWifiNetworkRulesEnabled()
        val shouldRestartMonitoring =
            serviceJob?.isActive != true ||
                intent?.getBooleanExtra(EXTRA_REAPPLY_POLICY, true) != false ||
                desiredMonitorWifiSsid != monitorWifiSsid
        if (shouldRestartMonitoring) {
            startNetworkMonitoring()
        } else {
            if (monitorWifiSsid &&
                currentNetworkState.networkType == NETWORK_TYPE_WIFI &&
                currentNetworkState.wifiSsid == null && currentNetworkState.wifiBssid == null
            ) {
                // Returning from permission settings (or opening the app after a boot without
                // background location) can make previously redacted WifiInfo readable. Request
                // a fresh inventory without resetting policy state or overriding manual DNS.
                initialNetworkInventoryPending = true
                networkCallback?.let {
                    NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(this, it)
                }
                registerNetworkCallback()
            }
            updateNotification()
        }

        Log.d(TAG, "Network monitoring service started")
        return START_STICKY
    }

    private fun startNetworkMonitoring() {
        stopNetworkMonitoring()
        monitorWifiSsid = prefsManager.areWifiNetworkRulesEnabled()
        initialNetworkInventoryPending = monitorWifiSsid
        forceNextResolvedState = true
        registerNetworkCallback()

        val detectedState = NetworkTypeDetectionUtils.getCurrentNetworkState(
            context = this,
            includeWifiSsid = monitorWifiSsid
        )
        handleNetworkStateChange(detectedState)

        serviceJob = serviceScope.launch {
            while (isActive) {
                try {
                    delay(CHECK_INTERVAL_MS.milliseconds)
                    checkNetworkState()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic network type check", e)
                    delay(CHECK_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            networkCallback = NetworkTypeDetectionUtils.createNetworkStateCallback(
                context = this,
                includeWifiSsid = monitorWifiSsid,
                onNetworkStateChanged = { newNetworkState ->
                    serviceScope.launch {
                        handleNetworkStateChange(newNetworkState)
                    }
                }
            )

            networkCallback?.let { callback ->
                NetworkTypeDetectionUtils.registerNetworkTypeCallback(this, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun checkNetworkState() {
        handleNetworkStateChange(
            NetworkTypeDetectionUtils.getCurrentNetworkState(
                context = this,
                includeWifiSsid = monitorWifiSsid
            )
        )
    }

    private fun handleNetworkStateChange(
        newNetworkState: DetectedNetworkState,
        force: Boolean = false
    ) {
        val vpnOverrideActive =
            prefsManager.isVpnDetectionEnabled() && VpnDetectionUtils.isVpnActive(this)
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
        if (!applyForced && newNetworkState == currentNetworkState && !vpnOverrideChanged) return

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
            updateNotification()
            return
        }

        NetworkDnsAutomation.apply(this, prefsManager, newNetworkState)

        updateNotification()
    }

    private fun scheduleWifiIdentityFallback() {
        if (wifiIdentityFallbackJob?.isActive == true) return
        Log.d(TAG, "Waiting briefly for location-aware Wi-Fi capabilities")
        wifiIdentityFallbackJob = serviceScope.launch {
            delay(CHECK_INTERVAL_MS.milliseconds)
            wifiIdentityFallbackJob = null
            handleNetworkStateChange(
                NetworkTypeDetectionUtils.getCurrentNetworkState(
                    context = this@NetworkMonitoringService,
                    includeWifiSsid = true
                ),
                force = true
            )
        }
    }

    private fun startForegroundForCurrentConfiguration() {
        // This call intentionally remains outside the monitoring-restart branch. When Android
        // starts the service after BOOT_COMPLETED, background location permission allows SSID
        // matching immediately. Without it, opening the app brings the process to the foreground
        // and this call then
        // upgrades the service to the location foreground-service type, restoring SSID matching
        // even when the monitoring job itself is already running.
        val notification = createNotification()
        val baseType = baseForegroundServiceType()
        val canUseLocationType =
            prefsManager.areWifiNetworkRulesEnabled() &&
                    PermissionUtils.canAccessWifiSsid(this)
        val requestedType = if (canUseLocationType) {
            baseType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            baseType
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, requestedType)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location foreground service unavailable; SSID data will be redacted", e)
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, baseType)
        }
    }

    private fun startForegroundBase() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            baseForegroundServiceType()
        )
    }

    private fun baseForegroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.network_monitoring_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.network_monitoring_channel_description)
            setShowBadge(false)
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (currentNetworkState.networkType) {
            NETWORK_TYPE_WIFI -> getString(R.string.network_type_wifi)
            NETWORK_TYPE_MOBILE -> getString(R.string.network_type_mobile)
            NETWORK_TYPE_NONE -> getString(R.string.network_type_none)
            else -> getString(R.string.network_type_unknown)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.network_monitoring_notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNetworkMonitoring()
        serviceScope.cancel()

        Log.d(TAG, "Network monitoring service destroyed")
    }

    private fun stopNetworkMonitoring() {
        serviceJob?.cancel()
        serviceJob = null
        wifiIdentityFallbackJob?.cancel()
        wifiIdentityFallbackJob = null
        initialNetworkInventoryPending = false
        forceNextResolvedState = false
        networkCallback?.let { callback ->
            NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(this, callback)
        }
        networkCallback = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
