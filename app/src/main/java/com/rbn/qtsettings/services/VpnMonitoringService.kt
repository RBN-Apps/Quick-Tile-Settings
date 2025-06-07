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
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VpnMonitoringService : Service() {

    companion object {
        private const val TAG = "VpnMonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_monitoring_channel"
        private const val CHECK_INTERVAL_MS = 3000L // Check every 3 seconds

        fun startService(context: Context) {
            val intent = Intent(context, VpnMonitoringService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VpnMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var prefsManager: PreferencesManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var serviceJob: Job? = null
    private var isVpnConnected = false

    private val servicePrefs: SharedPreferences by lazy {
        getSharedPreferences("vpn_detection_shared_state", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
        createNotificationChannel()
        Log.d(TAG, "VPN monitoring service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefsManager.isVpnDetectionEnabled() || prefsManager.getVpnDetectionMode() != BACKGROUND_DETECTION) {
            Log.d(TAG, "VPN detection disabled or not in background mode, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Log.w(TAG, "No WRITE_SECURE_SETTINGS permission, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startVpnMonitoring()

        Log.d(TAG, "VPN monitoring service started")
        return START_STICKY
    }

    private fun startVpnMonitoring() {
        val currentVpnState = VpnDetectionUtils.isVpnActive(this)
        val existingVpnState = getVpnPreviousState()

        if (currentVpnState && existingVpnState == null) {
            val currentMode = VpnDetectionUtils.getCurrentPrivateDnsMode(this)
            val currentHostname = VpnDetectionUtils.getCurrentPrivateDnsHostname(this)

            if (currentMode != DNS_MODE_OFF) {
                saveVpnPreviousState(currentMode, currentHostname)
                VpnDetectionUtils.setPrivateDnsOff(this)
                Log.i(TAG, "Service started with VPN active: Set Private DNS to off")
            }
        } else if (!currentVpnState && existingVpnState != null) {
            VpnDetectionUtils.restorePrivateDns(
                this,
                existingVpnState.first,
                existingVpnState.second
            )
            clearVpnPreviousState()
            Log.i(
                TAG,
                "Service started with VPN inactive: Restored Private DNS to ${existingVpnState.first}"
            )
        }

        isVpnConnected = currentVpnState

        registerNetworkCallback()

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    checkVpnStatus()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic VPN check", e)
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            networkCallback = VpnDetectionUtils.createVpnNetworkCallback(
                onVpnConnected = {
                    CoroutineScope(Dispatchers.Main).launch {
                        handleVpnStateChange(true)
                    }
                },
                onVpnDisconnected = {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        val stillConnected =
                            VpnDetectionUtils.isVpnActive(this@VpnMonitoringService)
                        if (!stillConnected) {
                            handleVpnStateChange(false)
                        }
                    }
                }
            )

            networkCallback?.let { callback ->
                VpnDetectionUtils.registerVpnCallback(this, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun checkVpnStatus() {
        val currentVpnState = VpnDetectionUtils.isVpnActive(this)
        if (currentVpnState != isVpnConnected) {
            CoroutineScope(Dispatchers.Main).launch {
                handleVpnStateChange(currentVpnState)
            }
        }
    }

    private fun handleVpnStateChange(vpnConnected: Boolean) {
        if (vpnConnected == isVpnConnected) return // No change

        isVpnConnected = vpnConnected

        if (vpnConnected) {
            onVpnConnected()
        } else {
            onVpnDisconnected()
        }

        updateNotification()
    }

    private fun onVpnConnected() {
        try {
            val currentMode = VpnDetectionUtils.getCurrentPrivateDnsMode(this)
            val currentHostname = VpnDetectionUtils.getCurrentPrivateDnsHostname(this)

            saveVpnPreviousState(currentMode, currentHostname)

            if (currentMode != DNS_MODE_OFF) {
                VpnDetectionUtils.setPrivateDnsOff(this)
                Log.i(TAG, "VPN connected: Set Private DNS to off")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling VPN connection", e)
        }
    }

    private fun onVpnDisconnected() {
        try {
            val (previousMode, previousHostname) = getVpnPreviousState() ?: return

            VpnDetectionUtils.restorePrivateDns(this, previousMode, previousHostname)
            clearVpnPreviousState()

            Log.i(TAG, "VPN disconnected: Restored Private DNS to $previousMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling VPN disconnection", e)
        }
    }

    private fun saveVpnPreviousState(mode: String, hostname: String?) {
        servicePrefs.edit {
            putString("vpn_previous_dns_mode", mode)
            putString("vpn_previous_dns_hostname", hostname)
        }
    }

    private fun getVpnPreviousState(): Pair<String, String?>? {
        val mode = servicePrefs.getString("vpn_previous_dns_mode", null)
        return mode?.let {
            Pair(it, servicePrefs.getString("vpn_previous_dns_hostname", null))
        }
    }

    private fun clearVpnPreviousState() {
        servicePrefs.edit {
            remove("vpn_previous_dns_mode")
            remove("vpn_previous_dns_hostname")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.vpn_monitoring_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.vpn_monitoring_channel_description)
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

        val statusText = if (isVpnConnected) {
            getString(R.string.vpn_monitoring_status_connected)
        } else {
            getString(R.string.vpn_monitoring_status_disconnected)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_monitoring_notification_title))
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

        networkCallback?.let { callback ->
            VpnDetectionUtils.unregisterVpnCallback(this, callback)
        }

        Log.d(TAG, "VPN monitoring service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}