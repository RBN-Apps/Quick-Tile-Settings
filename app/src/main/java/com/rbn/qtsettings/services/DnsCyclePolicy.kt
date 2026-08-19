package com.rbn.qtsettings.services

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON

internal data class DnsCycleState(
    val mode: String,
    val hostname: String? = null
)

internal object DnsCyclePolicy {
    fun nextState(
        currentMode: String,
        currentHostname: String?,
        includeOff: Boolean,
        includeAuto: Boolean,
        hostnames: List<DnsHostnameEntry>
    ): DnsCycleState? {
        val enabledStates = buildList {
            if (includeOff) add(DnsCycleState(DNS_MODE_OFF))
            if (includeAuto) add(DnsCycleState(DNS_MODE_AUTO))
            hostnames.forEach { entry -> add(DnsCycleState(DNS_MODE_ON, entry.hostname)) }
        }
        if (enabledStates.isEmpty()) return null

        val currentIndex = if (currentMode == DNS_MODE_ON) {
            enabledStates.indexOfFirst {
                it.mode == DNS_MODE_ON && it.hostname == currentHostname
            }
        } else {
            enabledStates.indexOfFirst { it.mode == currentMode }
        }
        return enabledStates[(currentIndex + 1) % enabledStates.size]
    }
}
