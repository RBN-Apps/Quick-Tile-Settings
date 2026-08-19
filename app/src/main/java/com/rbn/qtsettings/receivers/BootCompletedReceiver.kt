package com.rbn.qtsettings.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val preferences = PreferencesManager.getInstance(context)

        if (
            preferences.isVpnDetectionEnabled() &&
            preferences.getVpnDetectionMode() == BACKGROUND_DETECTION
        ) {
            VpnMonitoringService.startService(context)
        }

        if (
            preferences.isNetworkTypeDetectionEnabled() &&
            preferences.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION
        ) {
            NetworkMonitoringService.startService(context)
        }
    }
}
