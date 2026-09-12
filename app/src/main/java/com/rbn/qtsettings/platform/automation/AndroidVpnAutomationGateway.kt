package com.rbn.qtsettings.platform.automation

import android.content.Context
import androidx.core.content.edit
import com.rbn.qtsettings.feature.automation.domain.VpnAutomationGateway
import com.rbn.qtsettings.feature.automation.domain.VpnDnsState
import com.rbn.qtsettings.utils.VpnDetectionUtils

class AndroidVpnAutomationGateway(context: Context) : VpnAutomationGateway {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("vpn_detection_shared_state", Context.MODE_PRIVATE)
    override fun currentDns() = VpnDnsState(VpnDetectionUtils.getCurrentPrivateDnsMode(context), VpnDetectionUtils.getCurrentPrivateDnsHostname(context))
    override fun savedDns(): VpnDnsState? = preferences.getString("vpn_previous_dns_mode", null)?.let {
        VpnDnsState(it, preferences.getString("vpn_previous_dns_hostname", null))
    }
    override fun save(state: VpnDnsState) = preferences.edit {
        putString("vpn_previous_dns_mode", state.mode)
        putString("vpn_previous_dns_hostname", state.hostname)
    }
    override fun clear() = preferences.edit {
        remove("vpn_previous_dns_mode")
        remove("vpn_previous_dns_hostname")
    }
    override fun disableDns() = VpnDetectionUtils.setPrivateDnsOff(context)
    override fun restoreDns(state: VpnDnsState) = VpnDetectionUtils.restorePrivateDns(context, state.mode, state.hostname)
}
