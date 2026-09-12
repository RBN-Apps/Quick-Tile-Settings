package com.rbn.qtsettings.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.rbn.qtsettings.MainActivity
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.platform.automation.NetworkMonitor
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.PermissionUtils

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

    private lateinit var prefsManager: AutomationSettingsSource
    private lateinit var monitor: NetworkMonitor
    private val currentNetworkState get() = monitor.currentNetworkState
    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext).automationStore
        monitor = NetworkMonitor(this, prefsManager, ::updateNotification)
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
            !monitor.running ||
                intent?.getBooleanExtra(EXTRA_REAPPLY_POLICY, true) != false ||
                desiredMonitorWifiSsid != monitor.monitorWifiSsid
        if (shouldRestartMonitoring) {
            monitor.startNetworkMonitoring()
        } else {
            monitor.refreshRedactedIdentity()
        }

        Log.d(TAG, "Network monitoring service started")
        return START_STICKY
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
        monitor.close()

        Log.d(TAG, "Network monitoring service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
