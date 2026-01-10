package com.rbn.qtsettings.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.rbn.qtsettings.MainActivity
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
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

class NetworkMonitoringService : Service() {

    companion object {
        private const val TAG = "NetworkMonitoringService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "network_monitoring_channel"
        private const val CHECK_INTERVAL_MS = 3000L // Check every 3 seconds

        fun startService(context: Context) {
            val intent = Intent(context, NetworkMonitoringService::class.java)
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
    private var currentNetworkType: String = NETWORK_TYPE_NONE
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val servicePrefs: SharedPreferences by lazy {
        getSharedPreferences("network_type_detection_shared_state", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
        createNotificationChannel()
        Log.d(TAG, "Network monitoring service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefsManager.isNetworkTypeDetectionEnabled() ||
            prefsManager.getNetworkTypeDetectionMode() != BACKGROUND_DETECTION) {
            Log.d(TAG, "Network type detection disabled or not in background mode, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Log.w(TAG, "No WRITE_SECURE_SETTINGS permission, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        currentNetworkType = NetworkTypeDetectionUtils.getCurrentNetworkType(this)
        startForeground(NOTIFICATION_ID, createNotification())
        startNetworkMonitoring()

        Log.d(TAG, "Network monitoring service started")
        return START_STICKY
    }

    private fun startNetworkMonitoring() {
        // Initialize current network type
        currentNetworkType = NetworkTypeDetectionUtils.getCurrentNetworkType(this)

        // Check if we need to restore saved state
        val savedNetworkType = getSavedNetworkType()
        if (savedNetworkType != null && savedNetworkType != currentNetworkType) {
            // Network changed while service was stopped, apply settings
            handleNetworkTypeChange(currentNetworkType)
        }

        // Save current network type
        saveNetworkType(currentNetworkType)

        registerNetworkCallback()

        // Periodic check as backup
        serviceJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkNetworkType()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic network type check", e)
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            networkCallback = NetworkTypeDetectionUtils.createNetworkTypeCallback(
                context = this,
                onNetworkTypeChanged = { newNetworkType ->
                    serviceScope.launch {
                        handleNetworkTypeChange(newNetworkType)
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

    private fun checkNetworkType() {
        val detectedType = NetworkTypeDetectionUtils.getCurrentNetworkType(this)
        if (detectedType != currentNetworkType) {
            handleNetworkTypeChange(detectedType)
        }
    }

    private fun handleNetworkTypeChange(newNetworkType: String) {
        if (newNetworkType == currentNetworkType) return // No change

        val oldNetworkType = currentNetworkType
        currentNetworkType = newNetworkType

        // Save current network type
        saveNetworkType(newNetworkType)

        Log.i(TAG, "Network type changed from $oldNetworkType to $newNetworkType")

        // Don't apply DNS changes if VPN is active (VPN takes priority)
        if (VpnDetectionUtils.isVpnActive(this)) {
            Log.d(TAG, "VPN is active, skipping DNS change for network type")
            updateNotification()
            return
        }

        // Apply DNS settings based on new network type
        when (newNetworkType) {
            NETWORK_TYPE_WIFI -> {
                val dnsState = prefsManager.getDnsStateOnWifi()
                val dnsHostname = prefsManager.getDnsHostnameOnWifi()
                NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
                    this,
                    NETWORK_TYPE_WIFI,
                    dnsState,
                    dnsHostname
                )
            }

            NETWORK_TYPE_MOBILE -> {
                val dnsState = prefsManager.getDnsStateOnMobile()
                val dnsHostname = prefsManager.getDnsHostnameOnMobile()
                NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
                    this,
                    NETWORK_TYPE_MOBILE,
                    dnsState,
                    dnsHostname
                )
            }

            NETWORK_TYPE_NONE -> {
                Log.d(TAG, "No active network, DNS settings unchanged")
            }
        }

        updateNotification()
    }

    private fun saveNetworkType(networkType: String) {
        servicePrefs.edit {
            putString("last_network_type", networkType)
        }
    }

    private fun getSavedNetworkType(): String? {
        return servicePrefs.getString("last_network_type", null)
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

        val statusText = when (currentNetworkType) {
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

        serviceJob?.cancel()
        serviceScope.cancel()

        networkCallback?.let { callback ->
            NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(this, callback)
        }

        Log.d(TAG, "Network monitoring service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
