package com.rbn.qtsettings.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rbn.qtsettings.MainActivity
import com.rbn.qtsettings.QuickTileSettingsApplication
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.NetworkDnsAutomation
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private lateinit var prefsManager: AutomationSettingsSource
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var serviceJob: Job? = null
    private var isVpnConnected = false

    private val vpnAutomation get() = (application as QuickTileSettingsApplication).container.vpnAutomation
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var generation = 0L

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext).automationStore
        createNotificationChannel()
        Log.d(TAG, "VPN monitoring service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Fulfil the foreground-service contract before any early-return validation below.
        startForeground(NOTIFICATION_ID, createNotification())

        if (!prefsManager.isVpnDetectionEnabled() || prefsManager.getVpnDetectionMode() != BACKGROUND_DETECTION) {
            Log.d(TAG, "VPN detection disabled or not in background mode, stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Log.w(TAG, "No WRITE_SECURE_SETTINGS permission, stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startVpnMonitoring()

        Log.d(TAG, "VPN monitoring service started")
        return START_STICKY
    }

    private fun startVpnMonitoring() {
        stopVpnMonitoring()

        val currentVpnState = VpnDetectionUtils.isVpnActive(this)
        vpnAutomation.initialize(currentVpnState, ::applyCurrentNetworkPolicy)

        isVpnConnected = currentVpnState

        registerNetworkCallback()

        serviceJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkVpnStatus()
                    delay(CHECK_INTERVAL_MS.milliseconds)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic VPN check", e)
                    delay(CHECK_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val registeredGeneration = generation
            networkCallback = VpnDetectionUtils.createVpnNetworkCallback(
                onVpnConnected = {
                    serviceScope.launch {
                        if (registeredGeneration == generation) handleVpnStateChange(VpnDetectionUtils.isVpnActive(this@VpnMonitoringService))
                    }
                },
                onVpnDisconnected = {
                    serviceScope.launch {
                        delay(1000.milliseconds)
                        val stillConnected =
                            VpnDetectionUtils.isVpnActive(this@VpnMonitoringService)
                        if (registeredGeneration == generation && !stillConnected) {
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
            serviceScope.launch {
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

    private fun onVpnConnected() = vpnAutomation.connected()

    private fun onVpnDisconnected() {
        vpnAutomation.disconnected(::applyCurrentNetworkPolicy)
    }

    private fun applyCurrentNetworkPolicy(): Boolean {
        if (!prefsManager.isNetworkTypeDetectionEnabled() || prefsManager.getNetworkTypeDetectionMode() != BACKGROUND_DETECTION) return false
        return NetworkDnsAutomation.apply(this, prefsManager,
            NetworkTypeDetectionUtils.getCurrentNetworkState(this, prefsManager.areWifiNetworkRulesEnabled()))
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
        stopVpnMonitoring()
        serviceScope.cancel()

        Log.d(TAG, "VPN monitoring service destroyed")
    }

    private fun stopVpnMonitoring() {
        generation++
        serviceJob?.cancel()
        serviceJob = null
        networkCallback?.let { callback ->
            VpnDetectionUtils.unregisterVpnCallback(this, callback)
        }
        networkCallback = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
