package com.rbn.qtsettings.utils

import android.content.Context
import android.util.Log
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.automation.data.readSettings
import com.rbn.qtsettings.feature.automation.data.toAutomationNetwork
import com.rbn.qtsettings.feature.automation.domain.AutomationPolicy
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.feature.dns.data.toPersisted

object NetworkDnsAutomation {
    private const val TAG = "NetworkDnsAutomation"

    fun resolveTarget(
        preferencesManager: PreferencesManager,
        networkState: DetectedNetworkState
    ): PrivateDnsTarget? {
        return resolveTarget(preferencesManager.automationStore, networkState)
    }

    fun resolveTarget(settings: AutomationSettingsSource, networkState: DetectedNetworkState): PrivateDnsTarget? {
        return AutomationPolicy.networkTarget(
            settings.readSettings(), networkState.toAutomationNetwork()
        )?.toPersisted()
    }

    fun apply(
        context: Context,
        preferencesManager: PreferencesManager,
        networkState: DetectedNetworkState
    ): Boolean {
        return apply(context, preferencesManager.automationStore, networkState)
    }

    fun apply(context: Context, settings: AutomationSettingsSource, networkState: DetectedNetworkState): Boolean {
        val target = resolveTarget(settings, networkState) ?: run {
            Log.d(TAG, "No safe DNS target for the current network; DNS settings unchanged")
            return false
        }
        return NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
            context = context,
            networkType = networkState.networkType,
            dnsMode = target.mode,
            dnsHostname = target.hostname
        )
    }
}
